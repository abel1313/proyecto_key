package com.ventas.key.hexagonal.botredes.dominio.modelo;

/**
 * Lo que una persona escribió en una red. En comentarios, {@code publicacionId} es el post y
 * {@code respondeA} el comentario padre (si es una respuesta). En mensajes directos los dos van null.
 */
public record Interaccion(RedSocial red, Canal canal, String id, String autorId,
                          String publicacionId, String respondeA, String texto, boolean traeAdjunto) {

    public static Interaccion comentario(RedSocial red, String commentId, String postId, String parentId,
                                         String autorId, String texto) {
        return new Interaccion(red, Canal.COMENTARIO, commentId, autorId, postId, parentId, texto, false);
    }

    public static Interaccion mensajeDirecto(RedSocial red, String mid, String autorId, String texto,
                                             boolean traeAdjunto) {
        return new Interaccion(red, Canal.MENSAJE_DIRECTO, mid, autorId, null, null, texto, traeAdjunto);
    }

    public boolean tieneTexto() {
        return texto != null && !texto.isBlank();
    }

    public boolean esMensajeDirecto() {
        return canal == Canal.MENSAJE_DIRECTO;
    }

    /** Clave para el control de abuso: por red y persona, para no mezclar IDs de redes distintas. */
    public String claveAbuso() {
        return red.codigo() + ":" + (autorId != null ? autorId : id);
    }
}
