package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/** Donde escribieron: en un grupo (donde se publica) o en un chat privado con el bot. */
public enum CanalWhatsapp {
    GRUPO, PRIVADO;

    public static CanalWhatsapp desde(String texto) {
        if ("GRUPO".equalsIgnoreCase(texto == null ? null : texto.trim())) {
            return GRUPO;
        }
        if ("PRIVADO".equalsIgnoreCase(texto == null ? null : texto.trim())) {
            return PRIVADO;
        }
        return null;
    }
}
