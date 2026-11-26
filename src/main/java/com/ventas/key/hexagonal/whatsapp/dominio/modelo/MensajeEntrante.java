package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import java.time.Instant;
import java.util.Set;

/**
 * Lo que un cliente escribió por WhatsApp, ya traducido del payload de Meta.
 *
 * @param waMessageId id del mensaje en Meta ("wamid...."): sirve para no procesar dos veces el mismo
 * @param telefono    número del cliente tal como lo manda Meta (se le contesta a ese mismo)
 * @param enviadoEn   la fecha y hora que manda Meta en el webhook: de aquí se cuenta la ventana de 24 h
 */
public record MensajeEntrante(String waMessageId, String telefono, String nombrePerfil, String tipo,
                              String texto, Instant enviadoEn) {

    private static final Set<String> TIPOS_CON_TEXTO = Set.of("text", "button", "interactive");

    /** Texto que el bot puede leer. Foto, audio, sticker, ubicación o contacto sin texto, no. */
    public boolean esTexto() {
        return texto != null && !texto.isBlank() && TIPOS_CON_TEXTO.contains(tipo);
    }
}
