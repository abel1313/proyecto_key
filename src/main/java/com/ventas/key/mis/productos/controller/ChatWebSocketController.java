package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.ChatMensaje;
import com.ventas.key.mis.productos.entity.ChatSesion;
import com.ventas.key.mis.productos.models.chat.*;
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
    private final SimpMessagingTemplate messagingTemplate;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public ChatWebSocketController(IChatSesionService sesionService,
                                   IChatMensajeService mensajeService,
                                   IChatNotificacionService notificacionService,
                                   SimpMessagingTemplate messagingTemplate) {
        this.sesionService = sesionService;
        this.mensajeService = mensajeService;
        this.notificacionService = notificacionService;
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
        String ip = extraerIp(accessor);
        String sesionId = sesionService.conectar(ip, request.getNombreUsuario());

        String nombre = sesionId != null ? resolverNombre(request.getNombreUsuario()) : "Visitante";

        messagingTemplate.convertAndSend(
            "/topic/chat.inicio." + request.getTempId(),
            new ChatConexionResponse(sesionId)
        );

        messagingTemplate.convertAndSend("/topic/chat.admin",
            ChatEventoAdmin.builder()
                .tipo("NUEVA_SESION")
                .sesionId(sesionId)
                .nombreUsuario(nombre)
                .build()
        );

        notificacionService.notificarNuevaSesion(sesionId, nombre);
        log.info("Cliente conectado al chat. Sesión: {}, IP: {}", sesionId, ip);
    }

    /**
     * Usuario envía un mensaje.
     * Payload: { sesionId, contenido }
     * Publica en: /topic/chat.admin (MENSAJE)
     */
    @MessageMapping("/chat.mensaje")
    public void mensaje(@Payload ChatMensajeRequest request) {
        Optional<ChatSesion> sesionOpt = sesionService.buscarSesionActiva(request.getSesionId());
        if (sesionOpt.isEmpty()) {
            log.warn("Mensaje en sesión inactiva o inexistente: {}", request.getSesionId());
            return;
        }
        sesionService.actualizarActividad(request.getSesionId());
        ChatMensaje saved = mensajeService.guardar(request.getSesionId(), "USUARIO", request.getContenido());

        messagingTemplate.convertAndSend("/topic/chat.admin",
            ChatEventoAdmin.builder()
                .tipo("MENSAJE")
                .sesionId(request.getSesionId())
                .nombreUsuario(sesionOpt.get().getNombreUsuario())
                .contenido(request.getContenido())
                .timestamp(saved.getTimestamp().format(FMT))
                .build()
        );
        notificacionService.notificarMensaje(request.getSesionId(), sesionOpt.get().getNombreUsuario(), request.getContenido());
        log.debug("Mensaje recibido en sesión {}", request.getSesionId());
    }

    /**
     * Admin responde a un usuario específico.
     * Payload: { sesionId, contenido }
     * Publica en: /topic/chat.usuario.{sesionId} (MENSAJE)
     */
    @MessageMapping("/chat.admin.responder")
    public void adminResponder(@Payload ChatAdminResponderRequest request) {
        sesionService.actualizarActividad(request.getSesionId());
        ChatMensaje saved = mensajeService.guardar(request.getSesionId(), "ADMIN", request.getContenido());

        messagingTemplate.convertAndSend("/topic/chat.usuario." + request.getSesionId(),
            ChatEventoUsuario.builder()
                .tipo("MENSAJE")
                .remitente("ADMIN")
                .contenido(request.getContenido())
                .timestamp(saved.getTimestamp().format(FMT))
                .build()
        );
        log.debug("Admin respondió en sesión {}", request.getSesionId());
    }

    /**
     * Admin notifica que está en el panel → se suspenden emails mientras esté conectado.
     */
    @MessageMapping("/chat.admin.conectado")
    public void adminConectado(SimpMessageHeaderAccessor accessor) {
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
