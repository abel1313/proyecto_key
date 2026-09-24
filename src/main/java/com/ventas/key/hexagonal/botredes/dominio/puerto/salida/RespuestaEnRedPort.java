package com.ventas.key.hexagonal.botredes.dominio.puerto.salida;

import com.ventas.key.hexagonal.botredes.dominio.modelo.RedSocial;

/**
 * Publica la respuesta en la red. Devuelven el id que asigna Meta, para reconocer después el eco.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]</p>
 */
public interface RespuestaEnRedPort {

    String responderComentario(RedSocial red, String commentId, String texto);

    String enviarMensaje(RedSocial red, String destinatarioId, String texto);
}
