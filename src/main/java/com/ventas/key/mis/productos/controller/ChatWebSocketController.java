package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.ChatMensaje;
import com.ventas.key.mis.productos.entity.ChatSesion;
import com.ventas.key.mis.productos.models.chat.*;
import com.ventas.key.mis.productos.service.ChatVivoBotService;
import com.ventas.key.mis.productos.service.api.IChatMensajeService;
import com.ventas.key.mis.productos.service.api.IChatNotificacionService;
import com.ventas.key.mis.productos.service.api.IChatSesionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Controller
@Slf4j
public class ChatWebSocketController {

    private final IChatSesionService sesionService;
    private final IChatMensajeService mensajeService;
    private final IChatNotificacionService notificacionService;
    private final ChatVivoBotService botService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public ChatWebSocketController(IChatSesionService sesionService,
                                   IChatMensajeService mensajeService,
                                   IChatNotificacionService notificacionService,
                                   ChatVivoBotService botService,
                                   SimpMessagingTemplate messagingTemplate) {
        this.sesionService = sesionService;
        this.mensajeService = mensajeService;
        this.notificacionService = notificacionService;
        this.botService = botService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Cliente solicita conexión → se crea sesión y se responde con el sesionId definitivo.
     * Payload: { tempId, nombreUsuario }
     * Responde en: /topic/chat.inicio.{tempId}
     * Publica en:  /topic/chat.admin (NUEVA_SESION)
     */
    @MessageMapping("/chat.conectar")
    public void conectar(@Payload ChatConectarRequest request, SimpMessageHeaderAccessor accessor) {
        log.info("[WS] /chat.conectar recibido — tempId={}, nombreUsuario={}, usuarioId={}", request.getTempId(), request.getNombreUsuario(), request.getUsuarioId());
        String ip = extraerIp(accessor);
        String sesionId = sesionService.conectar(ip, request.getNombreUsuario(), request.getUsuarioId());

        String nombre = sesionId != null ? resolverNombre(request.getNombreUsuario()) : "Visitante";

        log.info("[WS] Publicando sesionId={} en /topic/chat.inicio.{}", sesionId, request.getTempId());
        messagingTemplate.convertAndSend(
            "/topic/chat.inicio." + request.getTempId(),
            new ChatConexionResponse(sesionId)
        );

        log.info("[WS] Publicando NUEVA_SESION en /topic/chat.admin — sesionId={}, nombre={}", sesionId, nombre);
        messagingTemplate.convertAndSend("/topic/chat.admin",
            ChatEventoAdmin.builder()
                .tipo("NUEVA_SESION")
                .sesionId(sesionId)
                .nombreUsuario(nombre)
                .build()
        );

        notificacionService.notificarNuevaSesion(sesionId, nombre);
        log.info("[WS] Conexión completada — sesionId={}, ip={}", sesionId, ip);
    }

    /**
     * Usuario envía un mensaje.
     * Payload: { sesionId, contenido }
     * Publica en: /topic/chat.admin (MENSAJE)
     */
    @MessageMapping("/chat.mensaje")
    public void mensaje(@Payload ChatMensajeRequest request) {
        log.info("[WS] /chat.mensaje recibido — sesionId={}, contenido={}", request.getSesionId(), request.getContenido());
        // Reabre la sesion si el scheduler ya la habia cerrado por silencio. Antes se exigia estado
        // ACTIVA y el mensaje se descartaba sin avisarle a nadie: el cliente lo veia enviado y al
        // admin nunca le llegaba.
        Optional<ChatSesion> sesionOpt = sesionService.reactivarSesion(request.getSesionId());
        if (sesionOpt.isEmpty()) {
            // La sesion ya no existe en la base. Se le avisa al cliente para que abra una nueva y
            // reenvie lo que escribio, en vez de dejarlo creyendo que se mando.
            log.warn("[WS] Sesión inexistente: {} — se pide reconectar al cliente", request.getSesionId());
            messagingTemplate.convertAndSend("/topic/chat.usuario." + request.getSesionId(),
                ChatEventoUsuario.builder().tipo("SESION_CERRADA").build());
            return;
        }
        ChatMensaje saved = mensajeService.guardar(request.getSesionId(), "USUARIO", request.getContenido());

        String nombreUsuario = sesionOpt.get().getNombreUsuario();

        log.info("[WS] Publicando MENSAJE en /topic/chat.admin — sesionId={}", request.getSesionId());
        messagingTemplate.convertAndSend("/topic/chat.admin",
            ChatEventoAdmin.builder()
                .tipo("MENSAJE")
                .sesionId(request.getSesionId())
                .nombreUsuario(nombreUsuario)
                .remitente("USUARIO")
                .contenido(request.getContenido())
                .timestamp(saved.getTimestamp().format(FMT))
                .build()
        );

        // Ya no se manda correo en cada primer mensaje: ahora contesta el bot y el correo sale
        // solo cuando la conversacion se escala a una persona (el cliente la pide, se agota el
        // limite del bot, o el bot falla). Eso lo decide ChatVivoBotService.
        botService.atender(request.getSesionId(), sesionOpt.get().getUsuarioId(), nombreUsuario,
                saved.getId(), request.getContenido());
    }

    /**
     * Admin responde a un usuario específico.
     * Payload: { sesionId, contenido }
     * Publica en: /topic/chat.usuario.{sesionId} (MENSAJE)
     */
    @MessageMapping("/chat.admin.responder")
    public void adminResponder(@Payload ChatAdminResponderRequest request) {
        log.info("[WS] /chat.admin.responder recibido — sesionId={}, contenido={}", request.getSesionId(), request.getContenido());
        sesionService.actualizarActividad(request.getSesionId());
        // En cuanto el dueno escribe, la conversacion pasa a ser suya y el bot se calla. Es la
        // senal de "ya la tomo yo" que pidio el dueno, sin tener que apretar nada aparte.
        sesionService.cambiarModo(request.getSesionId(), IChatSesionService.MODO_HUMANO);
        ChatMensaje saved = mensajeService.guardar(request.getSesionId(), "ADMIN", request.getContenido());

        String topicUsuario = "/topic/chat.usuario." + request.getSesionId();
        log.info("[WS] Publicando MENSAJE en {} — contenido={}, timestamp={}", topicUsuario, request.getContenido(), saved.getTimestamp().format(FMT));
        messagingTemplate.convertAndSend(topicUsuario,
            ChatEventoUsuario.builder()
                .tipo("MENSAJE")
                .remitente("ADMIN")
                .contenido(request.getContenido())
                .timestamp(saved.getTimestamp().format(FMT))
                .build()
        );
    }

    /**
     * Admin notifica que está en el panel → se suspenden emails mientras esté conectado.
     */
    @MessageMapping("/chat.admin.conectado")
    public void adminConectado(SimpMessageHeaderAccessor accessor) {
        log.info("[WS] /chat.admin.conectado — wsSessionId={}", accessor.getSessionId());
        notificacionService.marcarAdminConectado(accessor.getSessionId());
    }

    private String extraerIp(SimpMessageHeaderAccessor accessor) {
        if (accessor.getSessionAttributes() != null) {
            Object ip = accessor.getSessionAttributes().get("clientIp");
            if (ip instanceof String s) return s;
        }
        return "desconocido";
    }

    private String resolverNombre(String nombre) {
        return (nombre != null && !nombre.isBlank()) ? nombre : "Visitante";
    }
}
