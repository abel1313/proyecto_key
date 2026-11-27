package com.ventas.key.hexagonal.whatsapp.infraestructura.salida.correo;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MotivoDeEscalada;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.AvisoAlAdministradorPort;
import com.ventas.key.mis.productos.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Manda el aviso al correo del administrador ({@code chat.admin-email}, variable {@code CHAT_ADMIN_EMAIL}, el mismo
 * que usa el chat en vivo). Lo que escribió el cliente va escapado: es texto de un tercero dentro de un HTML.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Gateway]</p>
 */
@Component
public class AvisoAdministradorCorreoAdapter implements AvisoAlAdministradorPort {

    private static final Logger log = LoggerFactory.getLogger(AvisoAdministradorCorreoAdapter.class);
    private static final int MAX_MENSAJE = 300;

    private final EmailService correo;
    private final String destinatario;
    /**
     * El correo NO se manda en el hilo que procesa el mensaje: un SMTP lento no debe retener el candado del número
     * ni llenar la cola del webhook (hallazgo del revisor, 2026-11-27). Un solo hilo y una cola corta: si se llena,
     * se descarta el aviso (la conversación ya quedó marcada en la pantalla).
     */
    private final ThreadPoolExecutor ejecutor = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(50), r -> {
                Thread t = new Thread(r, "whatsapp-aviso-correo");
                t.setDaemon(true);
                return t;
            });

    public AvisoAdministradorCorreoAdapter(EmailService correo, @Value("${chat.admin-email:}") String destinatario) {
        this.correo = correo;
        this.destinatario = destinatario;
    }

    @Override
    public void conversacionNecesitaPersona(String telefono, String nombre, String ultimoMensaje, MotivoDeEscalada motivo) {
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("WhatsApp: no hay correo de administrador configurado (chat.admin-email), no se manda el aviso");
            return;
        }
        String quien = nombre == null || nombre.isBlank() ? "Un cliente" : nombre;
        String html = "<p><strong>" + esc(quien) + "</strong> (+" + esc(telefono) + ") necesita a una persona por WhatsApp.</p>"
                + "<p><strong>Motivo:</strong> " + esc(motivo.descripcion()) + ".</p>"
                + "<p><strong>Su último mensaje:</strong><br>" + esc(recortar(ultimoMensaje)) + "</p>"
                + "<p>Contéstale desde <strong>Sistema → WhatsApp</strong>.</p>";
        String asunto = "WhatsApp: " + paraAsunto(quien) + " necesita a una persona";
        try {
            ejecutor.execute(() -> enviar(asunto, html));
        } catch (RejectedExecutionException e) {
            log.warn("WhatsApp: la cola de avisos por correo está llena, no se manda este aviso (la conversación sí quedó marcada)");
        }
    }

    private void enviar(String asunto, String html) {
        try {
            if (!correo.enviarTicket(destinatario, asunto, html)) {
                log.warn("WhatsApp: el correo de aviso al administrador no salió (la conversación sí quedó marcada)");
            }
        } catch (RuntimeException e) {
            log.warn("WhatsApp: falló el correo de aviso al administrador ({})", e.toString());
        }
    }

    /** El nombre lo escribe el propio cliente en su perfil de WhatsApp: sin saltos de línea ni caracteres raros, y corto. */
    static String paraAsunto(String nombre) {
        String limpio = nombre.replaceAll("[\\p{Cntrl}\\r\\n]+", " ").trim();
        return limpio.length() > 60 ? limpio.substring(0, 60) + "…" : limpio;
    }

    private static String recortar(String t) {
        if (t == null) {
            return "(sin texto)";
        }
        String limpio = t.replaceAll("\\s+", " ").trim();
        return limpio.length() > MAX_MENSAJE ? limpio.substring(0, MAX_MENSAJE) + "…" : limpio;
    }

    private static String esc(String t) {
        return HtmlUtils.htmlEscape(t == null ? "" : t);
    }
}
