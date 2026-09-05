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

import java.io.UnsupportedEncodingException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final NegocioService negocioService;
    private final LogoService logoService;

    @Value("${spring.mail.username}")
    private String remitente;

    // Ver el comentario largo de app.public-base-url en application.yml -- si queda vacío (no
    // configurado en este ambiente) el encabezado cae al ícono+texto de siempre, no rompe nada.
    @Value("${app.public-base-url:}")
    private String publicBaseUrl;

    // publicBaseUrl (arriba) es la URL del BACKEND (ej. qa.backend.novedades-jade.com.mx/mis-productos)
    // -- sirve para servir imagenes (logo) que el cliente de correo carga directo, pero NO para
    // links que el usuario debe abrir en el navegador: un link ahi cae en la API, no en la app, y
    // el navegador termina mostrando el JSON crudo del backend (ej. "Token invalido o expirado")
    // en vez de la pantalla de Angular (bug real 2026-09-03: "Ver promocion"/"Mi perfil" del
    // correo de promociones no llevaban a ningun lado por esto). api.cors_angular ya existe y es
    // la URL real del FRONTEND por ambiente (ConfigSocket la usa para CORS) -- se reusa aqui para
    // cualquier link que el correo mande a abrir en el navegador.
    @Value("${api.cors_angular:}")
    private String frontendBaseUrl;

    // Direccion del negocio para el pie de los correos -- no existe ningun campo de direccion en
    // ConfiguracionNegocio ni pantalla que lo administre todavia, asi que queda fija aqui por
    // ahora. Si mas adelante se agrega un campo editable en Sistema > Negocio & Contactos, mover
    // esto a leerlo de ahi (mismo criterio que ya se uso para whatsapp/facebook/instagram/tiktok).
    private static final String DIRECCION_NEGOCIO = "Luvianos, Estado de México, Salida a Hermiltepec";

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
            // Nombre de marca en vez de la direccion pelona -- estos correos son solo de aviso
            // (codigo de verificacion, estado de pedido, promociones), no se espera respuesta.
            // Sin Reply-To a proposito: no hay a donde redirigir una respuesta, el buzon de envio
            // no se revisa (pedido explicito del dueño 2026-09-05).
            helper.setFrom(remitente, "Novedades Jade");
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(envolverPlantilla(htmlContent), true);
            mailSender.send(message);
            log.info("Correo enviado a: {}", destinatario);
            return true;
        } catch (MessagingException | MailException | UnsupportedEncodingException e) {
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
     * Contenido del encabezado de marca. Pedido 2026-08-28: ya hay logo (LogoService, admin sube
     * y elige cuál usar en Personalización) -- si hay uno activo Y el ambiente tiene
     * app.public-base-url configurada, se usa como <img>; si falta cualquiera de las dos cosas
     * (todavía no se subió/activó un logo, o este ambiente no configuró su dominio público),
     * cae al ícono+texto de siempre. Nunca falla el envío del correo por esto -- ver el
     * try/catch, mismo criterio que filaRedesSociales().
     *
     * Un <img src="data:..."> (base64) NO sirve acá: muchos clientes de correo (Gmail incluido en
     * varios casos) lo bloquean o lo quitan -- el logo necesita vivir en una URL real, accesible
     * sin login, para que cargue en el correo (por eso GET /logos/{id}/imagen es público).
     */
    private String encabezadoMarca() {
        try {
            if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
                var logo = logoService.obtenerActivo().orElse(null);
                if (logo != null) {
                    String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
                    return "<img src=\"" + base + logo.getUrlImagen() + "\" width=\"150\" alt=\"Novedades Jade\" "
                            + "style=\"display:block;margin:0 auto;max-width:150px;height:auto;\">";
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar el logo activo para el encabezado del correo: {}", e.getMessage());
        }
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

    /**
     * Avisa al cliente que el estado de su pedido cambió (confirmado/entregado o cancelado).
     * Correo NO transaccional -- el llamador debe verificar {@code Cliente.recibirCorreos} antes
     * de invocar este método (ver PedidoServiceImpl.notificarSeguimientoPedido).
     * @return true si el envío fue exitoso, false si falló (no lanza excepción).
     */
    public boolean enviarSeguimientoPedido(String destinatario, String nombreCliente, Integer pedidoId, String estado) {
        String asunto = "Tu pedido #" + pedidoId + " — " + estado + " — Novedades Jade";
        String html = "<p style=\"margin:0 0 4px;\">Hola " + nombreCliente + ",</p>"
                + "<p style=\"margin:0 0 12px;\">El estado de tu pedido <strong>#" + pedidoId + "</strong> cambió a:</p>"
                + "<div style=\"text-align:center;margin:22px 0;\">"
                + "<span style=\"display:inline-block;background-color:#EAF6F0;color:#00875A;"
                + "font-size:20px;font-weight:700;padding:12px 26px;border-radius:10px;"
                + "font-family:Arial,Helvetica,sans-serif;\">" + estado + "</span>"
                + "</div>"
                + "<p style=\"margin:0;color:#6b7280;font-size:13px;\">Puedes consultar el detalle "
                + "completo desde \"Mis pedidos\" en la app.</p>";
        return enviarTicket(destinatario, asunto, html);
    }

    /**
     * Avisa al cliente que un producto que tiene en Favoritos volvió a tener stock.
     * Correo NO transaccional -- el llamador debe verificar {@code Cliente.recibirCorreos} antes
     * de invocar este método (ver VarianteServiceImpl.notificarRestock).
     * @return true si el envío fue exitoso, false si falló (no lanza excepción).
     */
    public boolean enviarAlertaStock(String destinatario, String nombreCliente, String nombreProducto, String detalleVariante) {
        String asunto = "¡Ya volvió el stock! — Novedades Jade";
        String detalle = (detalleVariante != null && !detalleVariante.isBlank()) ? " (" + detalleVariante + ")" : "";
        String html = "<p style=\"margin:0 0 4px;\">Hola " + nombreCliente + ",</p>"
                + "<p style=\"margin:0 0 12px;\">Buenas noticias: uno de tus favoritos ya está "
                + "disponible de nuevo:</p>"
                + "<div style=\"text-align:center;margin:22px 0;\">"
                + "<span style=\"display:inline-block;background-color:#EAF6F0;color:#00875A;"
                + "font-size:18px;font-weight:700;padding:12px 22px;border-radius:10px;"
                + "font-family:Arial,Helvetica,sans-serif;\">" + nombreProducto + detalle + "</span>"
                + "</div>"
                + "<p style=\"margin:0;color:#6b7280;font-size:13px;\">Corre, las existencias son "
                + "limitadas. Puedes verlo en tus Favoritos dentro de la app.</p>";
        return enviarTicket(destinatario, asunto, html);
    }

    /**
     * Confirmación de que el pedido se generó (2026-09-03) -- se manda SIEMPRE al crear el
     * pedido, sin importar quién lo generó (cliente o admin) ni Cliente.recibirCorreos: es un
     * comprobante, mismo criterio que notificarPedido/reenviarComprobante (el ticket de compra no
     * depende de la preferencia de correos, ver el comentario en Cliente.recibirCorreos).
     * @return true si el envío fue exitoso, false si falló (no lanza excepción).
     */
    public boolean enviarConfirmacionPedido(String destinatario, String nombreCliente, Integer pedidoId, Double total) {
        String asunto = "Recibimos tu pedido #" + pedidoId + " — Novedades Jade";
        String totalStr = total != null ? String.format("$%.2f", total) : "";
        String html = "<p style=\"margin:0 0 4px;\">Hola " + nombreCliente + ",</p>"
                + "<p style=\"margin:0 0 12px;\">¡Recibimos tu pedido! Ya lo estamos preparando.</p>"
                + "<div style=\"text-align:center;margin:22px 0;\">"
                + "<span style=\"display:inline-block;background-color:#EAF6F0;color:#00875A;"
                + "font-size:20px;font-weight:700;padding:12px 26px;border-radius:10px;"
                + "font-family:Arial,Helvetica,sans-serif;\">Pedido #" + pedidoId + "</span>"
                + (totalStr.isEmpty() ? "" : "<div style=\"margin-top:8px;color:#1f2937;font-size:15px;\">Total: <strong>" + totalStr + "</strong></div>")
                + "</div>"
                + "<p style=\"margin:0;color:#6b7280;font-size:13px;\">Puedes ver el detalle y el "
                + "estado de tu pedido desde \"Mis pedidos\" en la app.</p>";
        return enviarTicket(destinatario, asunto, html);
    }

    /**
     * Aviso al admin de que un cliente generó un pedido nuevo (2026-09-03) -- solo se dispara
     * cuando el pedido lo generó el propio cliente (ver PedidoServiceImpl.notificarPedidoCreado):
     * si lo generó un admin, ya lo sabe, no hace falta avisarle.
     * @return true si el envío fue exitoso, false si falló (no lanza excepción).
     */
    public boolean enviarAvisoNuevoPedido(String destinatarioAdmin, Integer pedidoId, String nombreCliente, Double total) {
        String asunto = "Nuevo pedido #" + pedidoId + " de " + nombreCliente + " — Novedades Jade";
        String totalStr = total != null ? String.format("$%.2f", total) : "";
        String html = "<p style=\"margin:0 0 12px;\"><strong>" + nombreCliente + "</strong> generó un pedido nuevo:</p>"
                + "<div style=\"text-align:center;margin:22px 0;\">"
                + "<span style=\"display:inline-block;background-color:#EAF6F0;color:#00875A;"
                + "font-size:20px;font-weight:700;padding:12px 26px;border-radius:10px;"
                + "font-family:Arial,Helvetica,sans-serif;\">Pedido #" + pedidoId + "</span>"
                + (totalStr.isEmpty() ? "" : "<div style=\"margin-top:8px;color:#1f2937;font-size:15px;\">Total: <strong>" + totalStr + "</strong></div>")
                + "</div>"
                + "<p style=\"margin:0;color:#6b7280;font-size:13px;\">Revísalo desde la sección de "
                + "Pedidos en la app.</p>";
        return enviarTicket(destinatarioAdmin, asunto, html);
    }

    /**
     * Avisa al cliente el día/hora/lugar del viaje de entrega de SU zona (2026-09-04,
     * EntregaZonaServiceImpl.programarEntrega) -- se manda a todos los clientes con un pedido
     * pendiente de esa zona en la semana, de un jalón, cuando el admin programa el viaje.
     * @return true si el envío fue exitoso, false si falló (no lanza excepción).
     */
    public boolean enviarProgramacionEntregaZona(String destinatario, String nombreCliente, Integer pedidoId,
                                                  String nombreZona, java.time.LocalDate fecha, String hora,
                                                  String puntoEncuentro) {
        String fechaStr = fecha.format(java.time.format.DateTimeFormatter.ofPattern("EEEE d 'de' MMMM",
                new java.util.Locale("es", "MX")));
        String asunto = "Entrega en " + nombreZona + " — " + fechaStr + " — Novedades Jade";
        String html = "<p style=\"margin:0 0 4px;\">Hola " + nombreCliente + ",</p>"
                + "<p style=\"margin:0 0 12px;\">Ya tenemos fecha para llevar tu pedido "
                + "<strong>#" + pedidoId + "</strong> a <strong>" + nombreZona + "</strong>:</p>"
                + "<div style=\"text-align:center;margin:22px 0;\">"
                + "<span style=\"display:inline-block;background-color:#EAF6F0;color:#00875A;"
                + "font-size:18px;font-weight:700;padding:12px 22px;border-radius:10px;"
                + "font-family:Arial,Helvetica,sans-serif;\">" + fechaStr + ", " + hora + "</span>"
                + "<div style=\"margin-top:10px;color:#1f2937;font-size:15px;\">📍 " + puntoEncuentro + "</div>"
                + "</div>"
                + "<p style=\"margin:0;color:#6b7280;font-size:13px;\">Te esperamos ahí ese día para "
                + "entregarte tu pedido.</p>";
        return enviarTicket(destinatario, asunto, html);
    }

    /**
     * Digest diario para el admin (StockBajoScheduler) con las variantes en o por debajo del
     * umbral configurado. {@code lineas} ya viene formateada por StockBajoService (nombre de
     * producto + talla/color + stock) para que EmailService no dependa de la entidad Variantes.
     * @return true si el envío fue exitoso, false si falló (no lanza excepción).
     */
    public boolean enviarAlertaStockBajo(String destinatario, List<String> lineas, int umbral) {
        String asunto = "Aviso de stock bajo (" + lineas.size() + ") — Novedades Jade";
        StringBuilder filas = new StringBuilder();
        for (String linea : lineas) {
            filas.append("<tr><td style=\"padding:8px 10px;border-bottom:1px solid #eef1f0;font-size:13px;\">")
                 .append(linea).append("</td></tr>");
        }
        String html = "<p style=\"margin:0 0 4px;\">Estas variantes están en o por debajo del umbral de "
                + umbral + " unidades:</p>"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"margin:16px 0;border:1px solid #eef1f0;border-radius:8px;overflow:hidden;\">"
                + filas
                + "</table>"
                + "<p style=\"margin:0;color:#6b7280;font-size:13px;\">Puedes ajustar el umbral desde "
                + "Sistema &gt; Negocio &amp; Contactos en la app.</p>";
        return enviarTicket(destinatario, asunto, html);
    }

    /**
     * Correo de promoción nueva, enviado en lote por PromocionServiceImpl a los clientes con el
     * checkbox de promociones activado. Correo NO transaccional -- el llamador debe verificar
     * {@code Cliente.recibirPromociones} antes de invocar este método (mismo criterio que
     * enviarAlertaStock/Cliente.recibirCorreos).
     * @return true si el envío fue exitoso, false si falló (no lanza excepción).
     */
    public boolean enviarPromocion(String destinatario, String nombreCliente, String descripcionPromocion, String imagenUrl) {
        String asunto = "🎉 Nueva promoción — Novedades Jade";
        String base = normalizar(frontendBaseUrl);
        String botonVerPromos = base != null
                ? "<div style=\"text-align:center;margin:6px 0 4px;\">"
                  + "<a href=\"" + base + "/promociones\" style=\"display:inline-block;background-color:#00875A;"
                  + "color:#ffffff;font-size:15px;font-weight:700;padding:12px 26px;border-radius:10px;"
                  + "text-decoration:none;font-family:Arial,Helvetica,sans-serif;\">Ver promoción</a></div>"
                : "<p style=\"margin:0 0 12px;\">Entra a la sección <strong>Promociones</strong> en la app "
                  + "para verla completa.</p>";
        String dondeDesactivar = base != null
                ? "en <a href=\"" + base + "/clientes/mis-datos\">Mi perfil</a>"
                : "en Mi perfil &gt; Mis datos";
        // La imagen es de la primera variante del combo (PromocionServiceImpl.enviarCorreoPromocionAsync)
        // -- opcional a proposito: una promocion puede no tener imagen cargada todavia, el correo
        // no debe romperse por eso, solo se ve mas plano (mismo criterio que el resto del correo,
        // que ya cae a texto cuando falta algo opcional).
        String imagenHtml = (imagenUrl != null && !imagenUrl.isBlank())
                ? "<div style=\"text-align:center;margin:0 0 18px;\">"
                  + "<img src=\"" + imagenUrl + "\" alt=\"" + descripcionPromocion + "\" width=\"320\" "
                  + "style=\"display:block;margin:0 auto;max-width:100%;height:auto;border-radius:12px;\">"
                  + "</div>"
                : "";
        String html = "<p style=\"margin:0 0 4px;\">Hola " + nombreCliente + ",</p>"
                + "<p style=\"margin:0 0 12px;\">¡Tenemos una promoción nueva para ti!</p>"
                + imagenHtml
                + "<div style=\"text-align:center;margin:22px 0;\">"
                + "<span style=\"display:inline-block;background-color:#EAF6F0;color:#00875A;"
                + "font-size:17px;font-weight:700;padding:12px 22px;border-radius:10px;"
                + "font-family:Arial,Helvetica,sans-serif;\">" + descripcionPromocion + "</span>"
                + "</div>"
                + botonVerPromos
                + "<p style=\"margin:16px 0 0;color:#6b7280;font-size:12px;\">Si ya no quieres recibir "
                + "correos de promociones, puedes desactivarlos " + dondeDesactivar
                + " (casilla \"Recibir promociones\").</p>";
        return enviarTicket(destinatario, asunto, html);
    }

    /** {@code null} si la url no está configurada en este ambiente (no rompe nada). */
    private String normalizar(String url) {
        if (url == null || url.isBlank()) return null;
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
