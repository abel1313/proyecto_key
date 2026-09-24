package com.ventas.key.hexagonal.botredes.infraestructura.salida.meta;

import com.ventas.key.hexagonal.botredes.dominio.modelo.RedSocial;
import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.RespuestaEnRedPort;
import com.ventas.key.mis.productos.redessociales.FacebookGraphClient;
import com.ventas.key.mis.productos.redessociales.InstagramGraphClient;
import org.springframework.stereotype.Component;

/**
 * Publica con la Graph API de Meta: comentarios con /comments (Facebook) o /replies (Instagram);
 * mensajes con /{page-id}/messages (Messenger) o /{ig-user-id}/messages (Instagram).
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Gateway]</p>
 */
@Component
public class RespuestaEnRedGraphAdapter implements RespuestaEnRedPort {

    private final FacebookGraphClient facebook;
    private final InstagramGraphClient instagram;

    public RespuestaEnRedGraphAdapter(FacebookGraphClient facebook, InstagramGraphClient instagram) {
        this.facebook = facebook;
        this.instagram = instagram;
    }

    @Override
    public String responderComentario(RedSocial red, String commentId, String texto) {
        return red == RedSocial.FACEBOOK
                ? facebook.responderComentario(commentId, texto)
                : instagram.responderComentario(commentId, texto);
    }

    @Override
    public String enviarMensaje(RedSocial red, String destinatarioId, String texto) {
        return red == RedSocial.FACEBOOK
                ? facebook.enviarMensajeDirecto(destinatarioId, texto)
                : instagram.enviarMensajeDirecto(destinatarioId, texto);
    }
}
