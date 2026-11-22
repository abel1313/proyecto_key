package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Estados de una publicacion de WhatsApp. Se guardan como texto en {@code publicacion_social.estado}.
 * NO existe PROGRAMADA a proposito: ese estado lo dispara el job de Facebook/Instagram/TikTok.
 */
public enum EstadoPublicacion {
    PENDIENTE, EN_PROCESO, PUBLICADA, ERROR, CANCELADA;

    public static EstadoPublicacion desde(String texto) {
        if (texto == null) {
            return PENDIENTE;
        }
        try {
            return valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PENDIENTE;
        }
    }
}
