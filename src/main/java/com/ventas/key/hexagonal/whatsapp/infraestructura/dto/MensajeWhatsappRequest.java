package com.ventas.key.hexagonal.whatsapp.infraestructura.dto;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.CanalWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MensajeWhatsapp;

/**
 * Lo que manda el bot: {@code canal} es GRUPO o PRIVADO; {@code ref} (J42) y {@code citaTexto} son opcionales; {@code mencion}
 * indica que mencionaron al bot (vacio = no).
 */
public record MensajeWhatsappRequest(String canal, String chat, String remitente, String texto, String ref, String citaTexto,
                                     Boolean mencion) {

    /** Texto y cita se recortan para que nadie mande un mensaje enorme que gaste tokens. */
    static final int MAX_TEXTO = 1000;

    public MensajeWhatsapp aDominio() {
        return new MensajeWhatsapp(CanalWhatsapp.desde(canal), chat, remitente, recortar(texto), ref, recortar(citaTexto),
                Boolean.TRUE.equals(mencion));
    }

    private static String recortar(String t) {
        return t == null || t.length() <= MAX_TEXTO ? t : t.substring(0, MAX_TEXTO);
    }
}
