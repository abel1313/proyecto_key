package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.dto.negocio.ContactosPublicosDto;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final NegocioService negocioService;

    @Value("${spring.mail.username}")
    private String remitente;

    // Direccion del negocio para el pie de los correos -- no existe ningun campo de direccion en
    // ConfiguracionNegocio ni pantalla que lo administre todavia, asi que queda fija aqui por
    // ahora. Si mas adelante se agrega un campo editable en Sistema > Negocio & Contactos, mover
    // esto a leerlo de ahi (mismo criterio que ya se uso para whatsapp/facebook/instagram/tiktok).
    private static final String DIRECCION_NEGOCIO = null;

    /**
     * Envía un correo HTML al destinatario, envuelto automáticamente en la plantilla de marca
     * (ver {@link #envolverPlantilla}) -- todos los llamadores de este metodo (los 4 de abajo y
     * los que arman su propio HTML, ej. tickets de venta/pedido) salen con el mismo encabezado y
     * pie de pagina, sin tener que tocar cada uno.
     * @return true si el envío fue exitoso, false si falló (no lanza excepción).
     */
    public boolean enviarTicket(String destinatario, String asunto, String htmlContent) {
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("EmailService: destinatario vacío, correo no enviado");
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(envolverPlantilla(htmlContent), true);
            mailSender.send(message);
            log.info("Correo enviado a: {}", destinatario);
            return true;
        } catch (MessagingException | MailException e) {
            // MailException (ej. MailSendException por timeout/conexion SMTP con OVH) es RuntimeException
            // sin relacion con MessagingException - si no se captura aqui, se escapa del metodo pese a
            // que el contrato de la clase (ver javadocs) es "nunca lanza excepcion", y en los callers
            // @Transactional (ej. UsuarioVerificacionService.solicitarCambioCorreo) hace rollback del
            // guardado que ya se habia hecho antes de mandar el correo.
            log.error("Error enviando correo a {}: {}", destinatario, e.getMessage());
            return false;
        }
    }

    /**
     * Encabezado/pie de marca compartido por TODOS los correos del sistema -- se pidio "buen
     * diseno" para que se vean bien al enviarse (2026-08-25). Sin logo todavia (no existe ningun
     * archivo de logo en el proyecto, confirmado con el dueno): el encabezado usa el nombre de la
     * marca en texto/tipografia, mismo criterio que ya usa el sidebar del front (icono + texto,
     * sin imagen). Si mas adelante se sube un logo real, solo hay que cambiar el <td> del
     * encabezado de este metodo -- ningun llamador de enviarTicket() necesita tocarse.
     *
     * Tabla + estilos inline a proposito (no <style> en <head>, no flexbox/grid): es lo unico que
     * se renderiza consistente en clientes de correo (Gmail, Outlook, Apple Mail). El degradado
     * del encabezado lleva background-color de respaldo para Outlook de escritorio, que no
     * soporta gradientes CSS.
     */
    private String envolverPlantilla(String contenidoHtml) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background-color:#f4f6f5;padding:24px 0;font-family:Arial,Helvetica,sans-serif;\">"
                + "<tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"max-width:480px;background-color:#ffffff;border-radius:12px;overflow:hidden;"
                + "box-shadow:0 2px 10px rgba(0,0,0,0.07);\">"
                + "<tr><td style=\"background-color:#00875A;background-image:linear-gradient(135deg,#00875A,#005C3D);"
                + "padding:28px 24px;text-align:center;\">" + encabezadoMarca() + "</td></tr>"
                // Contenido
                + "<tr><td style=\"padding:32px 28px;color:#1f2937;font-size:15px;line-height:1.6;"
                + "font-family:Arial,Helvetica,sans-serif;\">"
                + contenidoHtml
                + "</td></tr>"
                // Pie -- redes sociales (las que el negocio tenga configuradas) + direccion
                + "<tr><td style=\"padding:20px 28px 18px;background-color:#f9fafb;text-align:center;"
                + "border-top:1px solid #eef1f0;\">"
                + filaRedesSociales()
                + (DIRECCION_NEGOCIO != null && !DIRECCION_NEGOCIO.isBlank()
                        ? "<p style=\"margin:0 0 6px;font-size:12px;color:#6b7280;font-family:Arial,Helvetica,sans-serif;\">"
                          + "📍 " + DIRECCION_NEGOCIO + "</p>"
                        : "")
                + "<p style=\"margin:0;font-size:12px;color:#9ca3af;font-family:Arial,Helvetica,sans-serif;\">"
                + "Novedades Jade — Este es un correo automático, no respondas a este mensaje.</p>"
                + "</td></tr>"
                + "</table>"
                + "</td></tr></table>";
    }

    /**
     * Fila de links a las redes sociales activas del negocio -- solo muestra las que de verdad
     * tienen URL configurada en Sistema > Negocio & Contactos (ConfiguracionNegocio), no una
     * lista fija: si mañana se agrega o se quita una red ahí, el correo se actualiza solo.
     * Envuelto en try/catch porque enviarTicket() no debe fallar un envío completo (ej. un
     * código de verificación urgente) solo porque no se pudo leer esta config secundaria.
     */
    private String filaRedesSociales() {
        try {
            ContactosPublicosDto c = negocioService.getContactosPublicos();
            StringBuilder sb = new StringBuilder();
            agregarRedSocial(sb, c.getWhatsappUrl(), "💬", "WhatsApp");
            agregarRedSocial(sb, c.getFacebookUrl(), "📘", "Facebook");
            agregarRedSocial(sb, c.getInstagramUrl(), "📷", "Instagram");
            agregarRedSocial(sb, c.getTiktokUrl(), "🎵", "TikTok");
            if (sb.length() == 0) return "";
            return "<p style=\"margin:0 0 12px;\">" + sb + "</p>";
        } catch (Exception e) {
            log.warn("No se pudo cargar la configuracion de negocio para el pie del correo: {}", e.getMessage());
            return "";
        }
    }

    private void agregarRedSocial(StringBuilder sb, String url, String emoji, String nombre) {
        if (url == null || url.isBlank()) return;
        if (sb.length() > 0) sb.append("&nbsp;&nbsp;");
        sb.append("<a href=\"").append(url).append("\" style=\"text-decoration:none;font-size:20px;\" "
                + "title=\"").append(nombre).append("\">").append(emoji).append("</a>");
    }

    /**
     * Contenido del encabezado de marca -- hoy es solo ícono + texto porque no existe ningún
     * archivo de logo en el proyecto (confirmado con el dueño, 2026-08-25). Cuando exista uno,
     * cambiar ÚNICAMENTE este método: subir el archivo a algo con URL pública (ej. junto a las
     * imágenes de presentación que ya sirve el back, o al hosting del front) y reemplazar el
     * <div> del ícono por
     *   "<img src=\"https://.../logo.png\" width=\"150\" alt=\"Novedades Jade\" "
     *   + "style=\"display:block;margin:0 auto;\">"
     * -- un <img src="data:..."> (base64) NO sirve aquí: muchos clientes de correo (Gmail incluido
     * en varios casos) lo bloquean o lo quitan: el logo necesita vivir en una URL real, accesible
     * sin login, para que cargue en el correo. No hace falta tocar envolverPlantilla() ni ningún
     * llamador de enviarTicket() -- todos los correos del sistema recogen el cambio solos.
     */
    private String encabezadoMarca() {
        return "<div style=\"font-size:26px;line-height:1;\">🛍️</div>"
                + "<div style=\"color:#ffffff;font-size:20px;font-weight:700;letter-spacing:.3px;"
                + "margin-top:6px;font-family:Arial,Helvetica,sans-serif;\">Novedades Jade</div>";
    }

    /**
     * Caja destacada para un código de un solo vistazo (verificación, reset de contraseña,
     * reclamo de compra) -- mismo estilo en los 3 casos para que se reconozca de inmediato.
     */
    private String cajaCodigo(String codigo) {
        return "<div style=\"text-align:center;margin:22px 0;\">"
                + "<span style=\"display:inline-block;background-color:#EAF6F0;color:#00875A;"
                + "font-size:28px;font-weight:700;letter-spacing:5px;padding:14px 30px;"
                + "border-radius:10px;font-family:Arial,Helvetica,sans-serif;\">" + codigo + "</span>"
                + "</div>";
    }

    /**
     * Envía el código de verificación de correo (6 dígitos, expira en 15 minutos).
     * @return true si el envío fue exitoso, false si falló (no lanza excepción).
     */
    public boolean enviarCodigoVerificacion(String destinatario, String codigo) {
        String asunto = "Verifica tu correo — Novedades Jade";
        String html = "<p style=\"margin:0 0 4px;\">Tu código de verificación es:</p>"
                + cajaCodigo(codigo)
                + "<p style=\"margin:0;color:#6b7280;font-size:13px;\">Vence en 15 minutos. Si tú no "
                + "solicitaste esta verificación, ignora este correo.</p>";
        return enviarTicket(destinatario, asunto, html);
    }

    /**
     * Envía el código para restablecer contraseña (6 dígitos, expira en 15 minutos).
     * @return true si el envío fue exitoso, false si falló (no lanza excepción).
     */
    public boolean enviarCodigoResetPassword(String destinatario, String codigo) {
        String asunto = "Restablecer tu contraseña — Novedades Jade";
        String html = "<p style=\"margin:0 0 4px;\">Tu código para restablecer la contraseña es:</p>"
                + cajaCodigo(codigo)
                + "<p style=\"margin:0;color:#6b7280;font-size:13px;\">Vence en 15 minutos. Si tú no "
                + "solicitaste este cambio, ignora este correo — tu contraseña actual sigue siendo válida.</p>";
        return enviarTicket(destinatario, asunto, html);
    }

    /**
     * Envía el código para que el cliente agregue a su cuenta una venta de mostrador hecha
     * con ClienteSinRegistro (caso: no se identificó en el momento de la compra). El texto
     * evita la palabra "reclamo" -- en español suena a queja, no a "esta compra es mía".
     * @return true si el envío fue exitoso, false si falló (no lanza excepción).
     */
    public boolean enviarCodigoReclamoVenta(String destinatario, String codigo) {
        String asunto = "Agrega tu compra a tu cuenta — Novedades Jade";
        String html = "<p style=\"margin:0 0 4px;\">Gracias por tu compra. Para que quede asociada a tu "
                + "cuenta, entra a la app, inicia sesión y captura este código en la sección "
                + "\"Agregar mi compra\":</p>"
                + cajaCodigo(codigo)
                + "<p style=\"margin:0;color:#6b7280;font-size:13px;\">Este código solo se puede usar una "
                + "vez. Si tú no realizaste esta compra, ignora este correo.</p>";
        return enviarTicket(destinatario, asunto, html);
    }

    /**
     * Avisa al ganador de una rifa que ganó un premio.
     * @return true si el envío fue exitoso, false si falló (no lanza excepción).
     */
    public boolean enviarNotificacionGanador(String destinatario, String nombreGanador, String premio) {
        String asunto = "¡Ganaste! — Novedades Jade";
        String html = "<p style=\"margin:0 0 4px;\">Hola " + nombreGanador + ",</p>"
                + "<p style=\"margin:0 0 4px;\">🎉 ¡Felicidades! Ganaste en nuestra rifa:</p>"
                + "<div style=\"text-align:center;margin:22px 0;\">"
                + "<span style=\"display:inline-block;background-color:#EAF6F0;color:#00875A;"
                + "font-size:22px;font-weight:700;padding:14px 26px;border-radius:10px;"
                + "font-family:Arial,Helvetica,sans-serif;\">" + premio + "</span>"
                + "</div>"
                + "<p style=\"margin:0;color:#6b7280;font-size:13px;\">Nos pondremos en contacto contigo "
                + "para coordinar la entrega.</p>";
        return enviarTicket(destinatario, asunto, html);
    }
}
