package com.ventas.key.hexagonal.whatsapp.infraestructura.salida.correo;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MensajeWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.AvisoAdminWhatsappPort;
import com.ventas.key.mis.productos.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * [Hexagonal: Driven Adapter] [Clean: Gateway]
 *
 * <p>Avisa por correo a {@code chat.admin-email}, igual que el bot de redes sociales.
 */
@Component
public class AvisoAdminWhatsappCorreoAdapter implements AvisoAdminWhatsappPort {

    private static final Logger log = LoggerFactory.getLogger(AvisoAdminWhatsappCorreoAdapter.class);

    private final EmailService email;
    private final String adminEmail;
    private final long pausaMinutos;

    public AvisoAdminWhatsappCorreoAdapter(EmailService email, @Value("${chat.admin-email:}") String adminEmail,
                                           @Value("${redes.bot.pausa-minutos:30}") long pausaMinutos) {
        this.email = email;
        this.adminEmail = adminEmail;
        this.pausaMinutos = pausaMinutos;
    }

    @Override
    public void necesitaAtencion(MensajeWhatsapp m, String publicacionCodigo, String motivo) {
        if (adminEmail == null || adminEmail.isBlank()) {
            log.warn("No se pudo avisar al admin de un mensaje de WhatsApp -- chat.admin-email no configurado");
            return;
        }
        String donde = m.esGrupo() ? "el grupo «" + escapar(m.chat()) + "»" : "un chat privado de WhatsApp";
        String asunto = "Un cliente necesita que le contestes -- WhatsApp";
        String html = "<p>El bot de WhatsApp le contestó con un saludo y te toca a ti: " + escapar(motivo) + ".</p>"
                + "<p><b>Mensaje:</b> " + escapar(m.texto()) + "</p>"
                + "<p><b>De:</b> " + escapar(m.remitente()) + " en " + donde + "</p>"
                + (publicacionCodigo != null ? "<p><b>Publicación:</b> " + escapar(publicacionCodigo) + "</p>" : "")
                + "<p>Contéstale directamente desde WhatsApp. El bot no se mete en esa conversación durante "
                + pausaMinutos + " minutos.</p>";
        email.enviarTicket(adminEmail, asunto, html);
    }

    private static String escapar(String texto) {
        return texto == null ? "" : texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
