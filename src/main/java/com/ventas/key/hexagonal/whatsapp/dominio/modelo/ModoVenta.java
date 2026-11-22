package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/** Como se cierra la venta de una publicacion: en el grupo (solo apartar) o por privado (recomendado). */
public enum ModoVenta {
    GRUPO, PRIVADO;

    public static ModoVenta desde(String texto) {
        return "GRUPO".equalsIgnoreCase(texto) ? GRUPO : PRIVADO;
    }
}
