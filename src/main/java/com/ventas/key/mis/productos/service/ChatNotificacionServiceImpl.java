package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.service.api.IChatNotificacionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class ChatNotificacionServiceImpl implements IChatNotificacionService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${chat.admin-email:admin@novedades-jade.com.mx}")
    private String adminEmail;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${chat.forzar-notificacion:false}")
    private boolean forzarNotificacion;

    private final AtomicBoolean adminConectado = new AtomicBoolean(false);
    private volatile String adminWsSessionId = null;

    @Override
    public void notificarNuevaSesion(String sesionId, String nombreUsuario) {
        if (!forzarNotificacion && adminConectado.get()) return;
        String prefijo = forzarNotificacion ? "[DEV] " : "";
        enviarEmail(
            prefijo + "Chat: nuevo visitante — " + nombreUsuario,
            "Un visitante ha iniciado un chat en la tienda.\n\nNombre: " + nombreUsuario
                + "\nSesión ID: " + sesionId
                + "\n\nEntra al panel admin para responder."
        );
    }

    @Override
    public void notificarMensaje(String sesionId, String nombreUsuario, String contenido) {
        if (!forzarNotificacion) return;
        enviarEmail(
            "[DEV] Chat: mensaje de " + nombreUsuario,
            "Mensaje recibido en sesión de prueba.\n\nNombre: " + nombreUsuario
                + "\nMensaje: " + contenido
                + "\nSesión ID: " + sesionId
        );
    }

    @Override
    public void marcarAdminConectado(String wsSessionId) {
        adminConectado.set(true);
        adminWsSessionId = wsSessionId;
        log.info("Admin del chat conectado (ws={}) — emails suspendidos", wsSessionId);
    }

    @Override
    public void marcarAdminDesconectado() {
        adminConectado.set(false);
        adminWsSessionId = null;
        log.info("Admin del chat desconectado — emails activos");
    }

    @Override
    public boolean isAdminConectado() {
        return adminConectado.get();
    }

    @Override
    public boolean isAdminSession(String wsSessionId) {
        return wsSessionId != null && wsSessionId.equals(adminWsSessionId);
    }

    private void enviarEmail(String asunto, String texto) {
        if (mailSender == null || mailUsername.isBlank()) {
            log.warn("Email no configurado — notificación de chat omitida: {}", asunto);
            return;
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(mailUsername);
            mail.setTo(adminEmail);
            mail.setSubject(asunto);
            mail.setText(texto);
            mailSender.send(mail);
            log.info("Email de chat enviado a {}", adminEmail);
        } catch (Exception e) {
            log.error("Error enviando email de chat: {}", e.getMessage());
        }
    }
}
