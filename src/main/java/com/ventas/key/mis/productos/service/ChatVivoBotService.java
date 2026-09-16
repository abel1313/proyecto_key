package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.chatbot.ChatbotBlockService;
import com.ventas.key.mis.productos.chatbot.ChatbotChatVivoService;
import com.ventas.key.mis.productos.entity.ChatMensaje;
import com.ventas.key.mis.productos.models.chat.ChatEventoAdmin;
import com.ventas.key.mis.productos.models.chat.ChatEventoUsuario;
import com.ventas.key.mis.productos.service.api.IChatMensajeService;
import com.ventas.key.mis.productos.service.api.IChatNotificacionService;
import com.ventas.key.mis.productos.service.api.IChatSesionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Quien decide si un mensaje del chat en vivo lo contesta el bot o se le deja al dueño.
 *
 * Antes el chat en vivo NO tenía bot: el cliente escribía y se quedaba esperando a que una persona
 * entrara al panel. Ahora contesta el prompt del chatbot y sólo se escala a una persona cuando hace
 * falta de verdad — el cliente la pide, se agotó el límite del bot, o el bot falló.
 *
 * Los dos tiempos de espera son lo que evita que el bot y el dueño se pisen:
 *  - en modo BOT espera unos segundos, por si el dueño está en el panel y contesta él primero;
 *  - en modo HUMANO el turno es del dueño, y el bot sólo entra si no contestó en un minuto.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatVivoBotService {

    private final ChatbotChatVivoService bot;
    private final ChatbotBlockService blockService;
    private final IChatSesionService sesionService;
    private final IChatMensajeService mensajeService;
    private final IChatNotificacionService notificacionService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static final String REMITENTE_BOT = "BOT";
    private static final String REMITENTE_USUARIO = "USUARIO";
    private static final String REMITENTE_ADMIN = "ADMIN";

    // Margen para que el dueño alcance a contestar él mismo antes que el bot.
    private static final Duration ESPERA_MODO_BOT = Duration.ofSeconds(6);

    // En modo HUMANO el primer turno es del dueño. Si no contesta, el bot cubre para que el
    // cliente no se quede esperando sin respuesta.
    private static final Duration ESPERA_MODO_HUMANO = Duration.ofMinutes(1);

    private static final String AVISO_LIMITE =
            "Voy a pasar tu mensaje con una persona del negocio para que te atienda por aquí mismo. "
            + "Dame un momento 🙏";

    private static final String AVISO_FALLA =
            "Tuve un problema para responderte. Ya le avisé a una persona del negocio para que "
            + "te atienda por aquí mismo 🙏";

    /**
     * Se llama justo después de guardar el mensaje del cliente. No bloquea: programa la respuesta
     * y regresa, para no dejar esperando al WebSocket.
     */
    public void atender(String sesionId, Integer usuarioId, String nombreUsuario, Long mensajeId, String contenido) {
        boolean esperaDelDueno = IChatSesionService.MODO_HUMANO.equals(sesionService.modoDe(sesionId));
        Duration espera = esperaDelDueno ? ESPERA_MODO_HUMANO : ESPERA_MODO_BOT;
        log.info("Chat en vivo: sesión {} en modo {} — el bot contesta en {}s si nadie más lo hace",
                sesionId, esperaDelDueno ? "HUMANO" : "BOT", espera.toSeconds());

        // publishOn y no subscribeOn: lo de adentro lee y escribe en la base y manda correo, o sea
        // que bloquea. Tiene que correr en boundedElastic y no en el hilo del temporizador ni en el
        // de Netty, que son pocos y se usan para todo lo demas.
        Mono.delay(espera)
                .publishOn(Schedulers.boundedElastic())
                .flatMap(tick -> responder(sesionId, usuarioId, nombreUsuario, mensajeId, contenido, esperaDelDueno))
                .subscribe(
                    ignorado -> { },
                    error -> log.error("Chat en vivo: falló el bot en la sesión {}: {}", sesionId, error.getMessage())
                );
    }

    private Mono<Void> responder(String sesionId, Integer usuarioId, String nombreUsuario, Long mensajeId,
                                String contenido, boolean cubriendoAlDueno) {
        if (!sigueEsperandoRespuesta(sesionId, mensajeId)) return Mono.empty();

        // El límite protege el crédito de OpenAI. A diferencia del widget público, aquí NO se
        // bloquea al cliente: se escala para que lo atienda una persona.
        String claveLimite = claveLimite(sesionId, usuarioId);
        if (blockService.limiteMensajesExcedido(claveLimite)) {
            log.info("Chat en vivo: sesión {} pasó el límite del bot, se escala", sesionId);
            escalar(sesionId, nombreUsuario, "se agotó el límite del asistente", contenido, AVISO_LIMITE);
            return Mono.empty();
        }
        blockService.registrarMensaje(claveLimite);

        List<ChatMensaje> historial = mensajeService.obtenerHistorial(sesionId);
        return bot.responder(contenido, historial)
                // La respuesta de OpenAI llega en un hilo de Netty; guardar en la base y mandar
                // correo desde ahi lo bloquearia.
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(respuesta -> procesarRespuesta(sesionId, nombreUsuario, contenido, respuesta, cubriendoAlDueno))
                .onErrorResume(error -> {
                    log.error("Chat en vivo: OpenAI falló en la sesión {}: {}", sesionId, error.getMessage());
                    notificacionService.notificarFallaDelBot(sesionId, nombreUsuario, error.getMessage());
                    escalar(sesionId, nombreUsuario, "el asistente no pudo responder", contenido, AVISO_FALLA);
                    return Mono.empty();
                })
                .then();
    }

    private void procesarRespuesta(String sesionId, String nombreUsuario, String pregunta, String cruda,
                                   boolean cubriendoAlDueno) {
        boolean pideHumano = cruda != null && cruda.contains(ChatbotChatVivoService.MARCA_HUMANO);
        String texto = limpiarMarcadores(cruda);

        if (texto.isBlank()) {
            // El modelo contestó sólo con marcadores. Mejor un texto propio que un globo vacío.
            texto = pideHumano ? AVISO_LIMITE : "¿Me lo puedes repetir? No te entendí bien 🙏";
        }

        publicarDelBot(sesionId, nombreUsuario, texto);

        if (pideHumano) {
            sesionService.cambiarModo(sesionId, IChatSesionService.MODO_HUMANO);
            notificacionService.notificarEscalado(sesionId, nombreUsuario, "el cliente pidió una persona", pregunta);
            log.info("Chat en vivo: sesión {} escalada porque el cliente pidió una persona", sesionId);
            return;
        }

        if (cubriendoAlDueno) {
            // El minuto de gracia del dueño se paga UNA vez. Si no entró, la conversación vuelve a
            // ser del bot: sin esto la sesión se quedaba en HUMANO y cada mensaje siguiente tenía
            // que esperar otro minuto completo antes de que el bot contestara.
            sesionService.cambiarModo(sesionId, IChatSesionService.MODO_BOT);
            log.info("Chat en vivo: el dueño no entró en la sesión {}, el bot retoma la conversación", sesionId);
        }
    }

    // Escala y le avisa al cliente con un texto propio (sin pasar por el modelo: se usa cuando el
    // modelo no está disponible o ya no se le quiere llamar).
    private void escalar(String sesionId, String nombreUsuario, String motivo, String ultimoMensaje, String avisoAlCliente) {
        sesionService.cambiarModo(sesionId, IChatSesionService.MODO_HUMANO);
        publicarDelBot(sesionId, nombreUsuario, avisoAlCliente);
        notificacionService.notificarEscalado(sesionId, nombreUsuario, motivo, ultimoMensaje);
    }

    private void publicarDelBot(String sesionId, String nombreUsuario, String texto) {
        ChatMensaje guardado = mensajeService.guardar(sesionId, REMITENTE_BOT, texto);
        String timestamp = guardado.getTimestamp().format(FMT);

        messagingTemplate.convertAndSend("/topic/chat.usuario." + sesionId,
            ChatEventoUsuario.builder()
                .tipo("MENSAJE")
                .remitente(REMITENTE_BOT)
                .contenido(texto)
                .timestamp(timestamp)
                .build()
        );

        // También al panel del dueño: así ve en vivo lo que contestó el asistente y puede entrar
        // a corregir si hace falta.
        messagingTemplate.convertAndSend("/topic/chat.admin",
            ChatEventoAdmin.builder()
                .tipo("MENSAJE")
                .sesionId(sesionId)
                .nombreUsuario(nombreUsuario)
                .remitente(REMITENTE_BOT)
                .contenido(texto)
                .timestamp(timestamp)
                .build()
        );
    }

    /**
     * El bot sólo contesta si ese mensaje sigue siendo el último de la conversación. Si el dueño ya
     * respondió, se calla y la conversación queda marcada como suya. Si el cliente mandó otro
     * mensaje después, lo atiende la llamada de ese mensaje y no se contesta dos veces.
     */
    private boolean sigueEsperandoRespuesta(String sesionId, Long mensajeId) {
        Optional<ChatMensaje> ultimo = mensajeService.ultimoMensaje(sesionId);
        if (ultimo.isEmpty()) {
            log.warn("Chat en vivo: la sesión {} no tiene mensajes, no hay qué contestar", sesionId);
            return false;
        }
        ChatMensaje u = ultimo.get();

        if (REMITENTE_ADMIN.equals(u.getRemitente())) {
            sesionService.cambiarModo(sesionId, IChatSesionService.MODO_HUMANO);
            log.info("Chat en vivo: el dueño contestó primero en la sesión {}, el bot se queda callado", sesionId);
            return false;
        }
        if (!REMITENTE_USUARIO.equals(u.getRemitente())) {
            log.info("Chat en vivo: en la sesión {} el último mensaje ya es del {}, el bot no contesta de nuevo",
                    sesionId, u.getRemitente());
            return false;
        }
        boolean sigueSiendoElUltimo = u.getId() != null && u.getId().equals(mensajeId);
        if (!sigueSiendoElUltimo) {
            // Normal cuando el cliente escribió otra vez: lo atiende la llamada de ESE mensaje.
            log.info("Chat en vivo: en la sesión {} el cliente ya escribió otro mensaje ({} en vez de {}), "
                    + "lo contesta esa llamada", sesionId, u.getId(), mensajeId);
        }
        return sigueSiendoElUltimo;
    }

    /**
     * El tope de mensajes se cuenta por USUARIO, no por sesión: la sesión se cierra a los 5 minutos
     * de silencio y el navegador abre otra, así que contar por sesión regalaba 20 mensajes nuevos
     * cada 5 minutos y el tope no servía de nada. Sólo se cae a la sesión si no hay usuario.
     */
    private String claveLimite(String sesionId, Integer usuarioId) {
        return usuarioId != null ? "chat-vivo-usuario-" + usuarioId : "chat-vivo-sesion-" + sesionId;
    }

    private String limpiarMarcadores(String texto) {
        if (texto == null) return "";
        return texto
                .replace(ChatbotChatVivoService.MARCA_HUMANO, "")
                .replace("##FAREWELL##", "")
                .replaceAll("##BUSCAR\\[[^\\]]*\\]##", "")
                .trim();
    }
}
