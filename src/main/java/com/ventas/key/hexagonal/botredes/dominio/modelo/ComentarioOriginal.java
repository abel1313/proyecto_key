package com.ventas.key.hexagonal.botredes.dominio.modelo;

/** Quién escribió un comentario y en qué publicación, para pausar esa conversación. */
public record ComentarioOriginal(String autorId, String publicacionId) {
}
