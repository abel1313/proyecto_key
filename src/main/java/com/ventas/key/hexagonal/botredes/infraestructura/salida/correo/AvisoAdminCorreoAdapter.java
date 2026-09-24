package com.ventas.key.hexagonal.botredes.infraestructura.salida.correo;

import com.ventas.key.hexagonal.botredes.dominio.modelo.Canal;
import com.ventas.key.hexagonal.botredes.dominio.modelo.Interaccion;
import com.ventas.key.hexagonal.botredes.dominio.modelo.RedSocial;
import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.AvisoAdminPort;
import com.ventas.key.mis.productos.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Avisa por correo a {@code chat.admin-email}.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Gateway]</p>
 */
@Component
public class AvisoAdminCorreoAdapter implements AvisoAdminPort {

    private static final Logger log = LoggerFactory.getLogger(AvisoAdminCorreoAdapter.class);

    private final EmailService email;
    private final String adminEmail;
    private final long pausaMinutos;

    public AvisoAdminCorreoAdapter(EmailService email, @Value("${chat.admin-email:}") String adminEmail,
                                   @Value("${redes.bot.pausa-minutos:30}") long pausaMinutos) {
        this.email = email;
        this.adminEmail = adminEmail;
        this.pausaMinutos = pausaMinutos;
    }

    @Override
    public void necesitaAtencion(Interaccion i, String motivo) {
        if (adminEmail == null || adminEmail.isBlank()) {
            log.warn("No se pudo avisar al admin de {} {} -- chat.admin-email no configurado", i.canal(), i.id());
            return;
        }
        String red = i.red() == RedSocial.FACEBOOK ? "Facebook" : "Instagram";
        String donde = i.canal() == Canal.COMENTARIO ? "un comentario de " + red : "un mensaje directo de " + red;
        String asunto = "Un cliente necesita que le contestes -- " + donde;
        String html = "<p>El bot le contestó con un saludo y te toca a ti: " + escapar(motivo) + ".</p>"
                + "<p><b>Mensaje:</b> " + (i.tieneTexto() ? escapar(i.texto()) : "(foto, audio o sticker)") + "</p>"
                + (i.publicacionId() != null ? "<p><b>Publicación:</b> " + escapar(i.publicacionId()) + "</p>" : "")
                + "<p><b>Cliente (ID):</b> " + escapar(i.autorId()) + "</p>"
                + "<p>Contéstale directamente desde " + red + ". El bot no se mete en esa conversación durante "
                + pausaMinutos + " minutos después de tu última respuesta.</p>";
        email.enviarTicket(adminEmail, asunto, html);
    }

    private String escapar(String texto) {
        return texto == null ? "" : texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
