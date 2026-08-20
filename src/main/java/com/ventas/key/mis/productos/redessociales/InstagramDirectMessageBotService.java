package com.ventas.key.mis.productos.redessociales;

import com.ventas.key.mis.productos.chatbot.ChatbotBlockService;
import com.ventas.key.mis.productos.chatbot.ChatbotService;
import com.ventas.key.mis.productos.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

// Mismo patron que InstagramCommentBotService, aplicado a mensajes directos (DM) en vez de
// comentarios publicos -- mismo "cerebro" (ChatbotService), mismo control de abuso/limite de 20
// msj/hora (ChatbotBlockService, la clave es autorId igual que en comentarios), misma logica de
// saludo/escalar/pausar. Diferencias con el de comentarios:
// - No hay "post" ni "variante" asociada -- un DM no esta ligado a una publicacion, se contesta
//   sin ese contexto extra (varianteDelPost siempre null).
// - La pausa por respuesta manual es por autor unicamente (MensajePausa), no por autor+post.
// - Instagram notifica los mensajes que la propia pagina manda como eventos "echo" (is_echo=true)
//   -- ahi el remitente real es la cuenta del negocio y el destinatario es el cliente, al reves
//   que en un mensaje entrante normal.
@Service
@Slf4j
@RequiredArgsConstructor
public class InstagramDirectMessageBotService {

    private final ChatbotService chatbotService;
    private final ChatbotBlockService blockService;
    private final InstagramGraphClient instagramGraphClient;
    private final EmailService emailService;
    private final IMensajeDirectoSocialRepository mensajeDirectoSocialRepository;
    private final IMensajePausaRepository mensajePausaRepository;

    @Value("${instagram.account-id:}")
    private String igUserId;

    @Value("${chat.admin-email:}")
    private String adminEmail;

    public void procesarMensaje(String mid, String senderId, String recipientId, String texto, boolean esEcho) {
        if (esEcho) {
            // Es un mensaje que mando la propia cuenta -- nuestro propio bot, o un admin
            // contestando manualmente desde la app real de Instagram. El destinatario (recipientId)
            // es el cliente real de esa conversacion.
            detectarRespuestaManualYPausar(mid, recipientId);
            return;
        }
        if (texto == null || texto.isBlank() || mid == null || senderId == null) {
            return;
        }
        if (senderId.equals(igUserId)) {
            // Seguridad extra -- no deberia pasar sin esEcho=true, pero por si acaso nunca se
            // auto-contesta a si misma.
            return;
        }

        if (mensajeDirectoSocialRepository.findByMid(mid).isPresent()) {
            log.info("Mensaje directo IG {} ya fue procesado antes, se ignora el reenvío", mid);
            return;
        }
        if (mensajePausaRepository.existsByAutorId(senderId)) {
            log.info("Mensaje directo IG {} ignorado -- un admin ya intervino manualmente con este cliente", mid);
            return;
        }
        if (blockService.estaBloqueado(senderId) || blockService.estaCooldown(senderId)) {
            log.info("Mensaje directo IG {} ignorado -- autor {} en cooldown/bloqueado", mid, senderId);
            return;
        }
        if (blockService.limiteMensajesExcedido(senderId)) {
            log.info("Mensaje directo IG {} ignorado -- autor {} alcanzo el limite de mensajes por hora", mid, senderId);
            guardarRegistro(mid, senderId, texto, null, null);
            return;
        }
        blockService.registrarMensaje(senderId);

        boolean esPrimeraVez = !mensajeDirectoSocialRepository.existsByAutorId(senderId);

        String respuesta;
        try {
            respuesta = chatbotService.responderComentarioRedSocial(texto, null, esPrimeraVez).block();
        } catch (Exception e) {
            log.warn("Error consultando el chatbot para el mensaje directo IG {}: {}", mid, e.getMessage());
            return;
        }

        if (respuesta != null && respuesta.contains("##ESCALAR##")) {
            escalarPorCorreo(mid, senderId, texto);
            guardarRegistro(mid, senderId, texto, null, null);
            return;
        }

        boolean noComprendido = respuesta == null || respuesta.contains("##FAREWELL##");
        String respuestaLimpia = null;
        if (!noComprendido) {
            respuestaLimpia = respuesta.replaceAll("##BUSCAR\\[[^\\]]*\\]##", "").trim();
            if (respuestaLimpia.isBlank()) {
                noComprendido = true;
                respuestaLimpia = null;
            }
        }

        if (noComprendido) {
            blockService.registrarFarewell(senderId);
            log.info("Mensaje directo IG {} no se contesta (no comprendido)", mid);
            guardarRegistro(mid, senderId, texto, null, null);
            return;
        }

        String respuestaMid = null;
        try {
            respuestaMid = instagramGraphClient.enviarMensajeDirecto(senderId, respuestaLimpia);
            log.info("Mensaje directo IG {} respondido por el bot (primeraVez={})", mid, esPrimeraVez);
        } catch (Exception e) {
            log.warn("No se pudo responder el mensaje directo IG {}: {}", mid, e.getMessage());
            respuestaLimpia = null;
        }

        guardarRegistro(mid, senderId, texto, respuestaLimpia, respuestaMid);
    }

    private void detectarRespuestaManualYPausar(String mid, String clienteId) {
        if (clienteId == null || clienteId.isBlank()) {
            return;
        }
        if (mensajeDirectoSocialRepository.existsByRespuestaMid(mid)) {
            // Es el eco de nuestra propia respuesta (bot), no una intervencion manual.
            return;
        }
        if (!mensajePausaRepository.existsByAutorId(clienteId)) {
            MensajePausa pausa = new MensajePausa();
            pausa.setAutorId(clienteId);
            pausa.setFecha(LocalDateTime.now());
            mensajePausaRepository.save(pausa);
            log.info("Admin respondió manualmente por DM a {} -- bot pausado para ese cliente", clienteId);
        }
    }

    private void escalarPorCorreo(String mid, String senderId, String texto) {
        if (adminEmail == null || adminEmail.isBlank()) {
            log.warn("No se pudo escalar el mensaje directo IG {} -- chat.admin-email no configurado", mid);
            return;
        }
        String asunto = "Un cliente preguntó algo que el bot no supo responder -- Instagram DM";
        String html = "<p>Un cliente escribió por mensaje directo de Instagram y el bot no tenía suficiente "
                + "información en el catálogo para contestar con seguridad -- no se envió ninguna respuesta.</p>"
                + "<p><b>Mensaje:</b> " + escapeHtml(texto) + "</p>"
                + "<p><b>Cliente (ID):</b> " + escapeHtml(senderId) + "</p>"
                + "<p>Entra a Instagram y contesta directamente el mensaje.</p>";
        emailService.enviarTicket(adminEmail, asunto, html);
        log.info("Mensaje directo IG {} escalado por correo al admin (bot no tenía el dato)", mid);
    }

    private String escapeHtml(String texto) {
        return texto == null ? "" : texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void guardarRegistro(String mid, String autorId, String mensaje, String respuesta, String respuestaMid) {
        MensajeDirectoSocial registro = new MensajeDirectoSocial();
        registro.setMid(mid);
        registro.setRedSocial("instagram");
        registro.setAutorId(autorId);
        registro.setMensaje(mensaje);
        registro.setRespuesta(respuesta);
        registro.setRespuestaMid(respuestaMid);
        registro.setFecha(LocalDateTime.now());
        mensajeDirectoSocialRepository.save(registro);
    }
}
