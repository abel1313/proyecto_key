package com.ventas.key.hexagonal.whatsapp.infraestructura.salida.pedido;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.BorradorDePedido;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ResultadoDeCreacion;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.NotificacionesDePedidoPort;
import com.ventas.key.mis.productos.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Los correos del pedido por WhatsApp. Todo lo que escribió el cliente va escapado (es texto de un tercero dentro de un
 * HTML). Al dueño se le manda a {@code chat.admin-email} ({@code CHAT_ADMIN_EMAIL}), el mismo del aviso de R13.
 * Ningún método lanza excepción. Los correos salen en OTRO hilo (uno solo, cola corta): un SMTP lento no retiene el
 * candado del número ni frena otros mensajes; si la cola se llena, se descarta el aviso. Los datos personales del cliente
 * (nombre, correo) no se escriben en el log.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Gateway]</p>
 */
@Component
public class NotificacionesDePedidoCorreoAdapter implements NotificacionesDePedidoPort {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesDePedidoCorreoAdapter.class);
    private static final int MAX_MOTIVO = 400;

    private final ThreadPoolExecutor ejecutor = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(50), r -> {
                Thread t = new Thread(r, "whatsapp-pedido-correo");
                t.setDaemon(true);
                return t;
            });

    private final EmailService correo;
    private final String administrador;

    public NotificacionesDePedidoCorreoAdapter(EmailService correo, @Value("${chat.admin-email:}") String administrador) {
        this.correo = correo;
        this.administrador = administrador;
    }

    @Override
    public boolean ticketAlCliente(BorradorDePedido b, ResultadoDeCreacion.Creado c) {
        String html = "<p>Hola " + esc(b.nombre()) + ", ¡recibimos tu pedido por WhatsApp! 🎉</p>"
                + "<p><strong>Pedido #" + c.pedidoId() + "</strong> (Apartado)</p>"
                + tabla(c)
                + "<p>Tu pedido quedó <strong>apartado</strong>: no se paga nada ahora. Una persona del negocio se "
                + "pondrá en contacto contigo para acordar " + (b.recogerEnLocal() ? "cuándo pasas por él" : "el día y la hora de entrega")
                + " y el pago (solo efectivo o transferencia).</p>"
                + "<p>" + entrega(b) + "</p>"
                + "<p style=\"color:#6b7280;font-size:13px;\">Si necesitas cancelarlo, escríbenos por WhatsApp con el "
                + "número de pedido.</p>";
        return encolar("ticket del pedido " + c.pedidoId(), b.correo(), "Tu pedido #" + c.pedidoId() + " — Novedades Jade", html);
    }

    @Override
    public void ventaAlAdministrador(BorradorDePedido b, ResultadoDeCreacion.Creado c) {
        String html = "<p><strong>Nuevo Apartado por WhatsApp</strong> — pedido #" + c.pedidoId() + ".</p>"
                + datos(b) + tabla(c)
                + "<p>Contacta al cliente para acordar entrega y pago. Revísalo en <strong>Pedidos</strong>.</p>";
        enviarAlAdministrador("venta del pedido " + c.pedidoId(), "Pedido #" + c.pedidoId() + " por WhatsApp de " + paraAsunto(b.nombre()), html);
    }

    @Override
    public void falloAlAdministrador(BorradorDePedido b, String motivo) {
        String html = "<p><strong>El bot NO pudo crear un pedido por WhatsApp.</strong> No se hizo ningún apartado ni se "
                + "movió el stock. El cliente ya sabe que una persona lo contactará.</p>"
                + "<p><strong>Lo ocurrido:</strong> " + esc(recortar(motivo)) + "</p>"
                + "<p><strong>Datos que ya se tenían:</strong></p>" + datos(b) + tablaDeBorrador(b);
        enviarAlAdministrador("fallo de un pedido", "WhatsApp: no se pudo crear el pedido de " + paraAsunto(b.nombre()), html);
    }

    @Override
    public void cancelacionAlAdministrador(String telefonoWhatsapp, String nombre, int numeroDePedido) {
        String quien = nombre == null || nombre.isBlank() ? "Un cliente" : nombre;
        String html = "<p><strong>" + esc(quien) + "</strong> (+" + esc(telefonoWhatsapp) + ") pide cancelar el "
                + "<strong>pedido #" + numeroDePedido + "</strong> por WhatsApp.</p>"
                + "<p>El bot no cancela pedidos ni da información de ellos: solo anotó el número. Revisa que el pedido sea "
                + "de esa persona y cancélalo desde <strong>Pedidos</strong>.</p>";
        enviarAlAdministrador("solicitud de cancelar el pedido " + numeroDePedido,
                "WhatsApp: " + paraAsunto(quien) + " pide cancelar el pedido #" + numeroDePedido, html);
    }

    @Override
    public void topeAlAdministrador(String telefonoWhatsapp, String nombre, int abiertos) {
        String quien = nombre == null || nombre.isBlank() ? "Un cliente" : nombre;
        String html = "<p><strong>" + esc(quien) + "</strong> (+" + esc(telefonoWhatsapp) + ") quiso apartar otro pedido por "
                + "WhatsApp, pero ya tiene <strong>" + abiertos + "</strong> apartados abiertos (el tope es "
                + "<code>whatsapp.bot.max-apartados-abiertos</code>). No se creó otro; la conversación quedó marcada.</p>";
        enviarAlAdministrador("tope de apartados", "WhatsApp: " + paraAsunto(quien) + " llegó al tope de apartados", html);
    }

    // ---------------------------------------------------------------------------------------------------------

    private void enviarAlAdministrador(String queEs, String asunto, String html) {
        if (administrador == null || administrador.isBlank()) {
            log.warn("WhatsApp: no hay correo de administrador configurado (chat.admin-email), no se manda el correo de {}", queEs);
            return;
        }
        encolar(queEs, administrador, asunto, html);
    }

    /** @return true si quedó en camino (en la cola); el resultado real del SMTP solo queda en el log. */
    private boolean encolar(String queEs, String destino, String asunto, String html) {
        try {
            ejecutor.execute(() -> {
                try {
                    if (!correo.enviarTicket(destino, asunto, html)) {
                        log.warn("WhatsApp: el correo de {} no salió", queEs);
                    }
                } catch (RuntimeException e) {
                    log.warn("WhatsApp: el correo de {} falló ({})", queEs, e.toString());
                }
            });
            return true;
        } catch (RejectedExecutionException e) {
            log.warn("WhatsApp: la cola de correos del pedido está llena, no se manda el correo de {}", queEs);
            return false;
        }
    }

    static String paraAsunto(String nombre) {
        String limpio = nombre == null ? "" : nombre.replaceAll("[\\p{Cntrl}\\r\\n]+", " ").trim();
        return limpio.length() > 60 ? limpio.substring(0, 60) + "…" : limpio;
    }

    private static String datos(BorradorDePedido b) {
        return "<p>Cliente: <strong>" + esc(b.nombre()) + "</strong><br>Teléfono: " + esc(b.telefono())
                + " (WhatsApp +" + esc(b.telefonoWhatsapp()) + " · <a href=\"" + esc(b.urlWhatsapp()) + "\">abrir chat</a>)"
                + "<br>Correo: " + esc(b.correo()) + "<br>" + entrega(b) + "<br>Observaciones: " + esc(b.observaciones()) + "</p>";
    }

    private static String entrega(BorradorDePedido b) {
        return b.recogerEnLocal()
                ? "Recoge en el local (recibe: " + esc(b.recibe()) + ")"
                : "Entrega en " + esc(b.lugar() == null ? "" : b.lugar().nombre()) + ": " + esc(b.direccion())
                + " (recibe: " + esc(b.recibe()) + ")";
    }

    /** Las líneas y el total REALES que cobró el servicio de ventas. */
    private static String tabla(ResultadoDeCreacion.Creado c) {
        StringBuilder sb = new StringBuilder("<table style=\"border-collapse:collapse;width:100%;\">");
        for (ResultadoDeCreacion.LineaCobrada l : c.lineas()) {
            sb.append("<tr><td style=\"padding:4px 8px;\">").append(l.cantidad()).append(" × ").append(esc(l.nombre()))
                    .append("</td><td style=\"padding:4px 8px;text-align:right;\">").append(dinero(l.subtotal())).append("</td></tr>");
        }
        sb.append("<tr><td style=\"padding:4px 8px;\"><strong>Total</strong></td><td style=\"padding:4px 8px;text-align:right;\">")
                .append("<strong>").append(dinero(c.total())).append("</strong></td></tr></table>");
        return sb.toString();
    }

    private static String tablaDeBorrador(BorradorDePedido b) {
        return tabla(new ResultadoDeCreacion.Creado(0, b.total(), b.lineas().stream().map(l ->
                new ResultadoDeCreacion.LineaCobrada(l.nombre(), l.cantidad(), l.precioUnitario(), l.precioUnitario())).toList()));
    }

    private static String dinero(double d) {
        return "$" + String.format(Locale.US, "%,.2f", d);
    }

    private static String recortar(String t) {
        if (t == null) {
            return "(sin detalle)";
        }
        String limpio = t.replaceAll("\\s+", " ").trim();
        return limpio.length() > MAX_MOTIVO ? limpio.substring(0, MAX_MOTIVO) + "…" : limpio;
    }

    private static String esc(String t) {
        return HtmlUtils.htmlEscape(t == null ? "" : t);
    }
}
