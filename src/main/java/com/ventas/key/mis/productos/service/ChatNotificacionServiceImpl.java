package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.service.api.IChatNotificacionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    // Sesiones que ya recibieron notificación — evita emails duplicados en la misma sesión
    private final Set<String> sesionesNotificadas = ConcurrentHashMap.newKeySet();

    @Override
    public void notificarNuevaSesion(String sesionId, String nombreUsuario) {
        // Notificación va en el primer mensaje, no en la conexión
    }

    @Override
    public void notificarMensaje(String sesionId, String nombreUsuario, String contenido) {
        // En dev: siempre envía (para pruebas). En QA/prod: solo primer mensaje por sesión
        if (!forzarNotificacion && !sesionesNotificadas.add(sesionId)) return;
        String prefijo = forzarNotificacion ? "[DEV] " : "";
        enviarEmail(
            prefijo + "Chat: mensaje de " + nombreUsuario,
            "Tienes un nuevo mensaje en el chat.\n\nNombre: " + nombreUsuario
                + "\nMensaje: " + contenido
                + "\n\nEntra al panel admin para responder."
        );
    }

    // El escalado NO se deduplica por sesion a proposito: se avisa cada vez que la conversacion
    // pasa de BOT a HUMANO. El cambio de modo ya es de una sola via (cambiarModo no hace nada si
    // el modo es el mismo), asi que un escalado = un correo, y si el cliente vuelve horas despues
    // y escala otra vez, ese segundo aviso tambien llega.
    @Override
    public void notificarEscalado(String sesionId, String nombreUsuario, String motivo, String ultimoMensaje) {
        enviarEmail(
            "Chat: " + nombreUsuario + " necesita atención (" + motivo + ")",
            "El asistente dejó de atender esta conversación y ahora te toca a ti.\n\n"
                + "Cliente: " + nombreUsuario + "\n"
                + "Motivo: " + motivo + "\n"
                + "Último mensaje: " + (ultimoMensaje != null ? ultimoMensaje : "(sin texto)") + "\n\n"
                + "Entra a Sistema → Chat directo para responderle."
        );
    }

    // Una falla de OpenAI puede repetirse en cada mensaje mientras dure. Se avisa una vez cada
    // 30 minutos por conversacion para no llenar el correo del dueno con el mismo problema.
    private static final Duration ESPERA_AVISO_FALLA = Duration.ofMinutes(30);
    private final Map<String, Instant> ultimoAvisoFalla = new ConcurrentHashMap<>();

    @Override
    public void notificarFallaDelBot(String sesionId, String nombreUsuario, String detalle) {
        Instant ahora = Instant.now();
        Instant previo = ultimoAvisoFalla.get(sesionId);
        if (previo != null && previo.plus(ESPERA_AVISO_FALLA).isAfter(ahora)) return;
        ultimoAvisoFalla.put(sesionId, ahora);

        enviarEmail(
            "Chat: el asistente no pudo responder",
            "El asistente falló al contestar en el chat y la conversación quedó esperándote.\n\n"
                + "Cliente: " + nombreUsuario + "\n"
                + "Detalle técnico: " + detalle + "\n\n"
                + "Revisa el crédito y la llave de OpenAI. Mientras no se resuelva, los chats\n"
                + "los tienes que atender tú desde Sistema → Chat directo."
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
