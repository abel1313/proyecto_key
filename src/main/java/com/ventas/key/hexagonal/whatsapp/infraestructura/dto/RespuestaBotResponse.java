package com.ventas.key.hexagonal.whatsapp.infraestructura.dto;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.RespuestaBot;

/** Lo que recibe el bot: {@code accion} (RESPONDER, IGNORAR, ESCALADO, OFRECER_PRIVADO) y el texto a mandar ({@code null} si se ignora). */
public record RespuestaBotResponse(String accion, String respuesta, Integer publicacionId, String modoVenta) {

    public static RespuestaBotResponse de(RespuestaBot r) {
        return new RespuestaBotResponse(r.accion().name(), r.respuesta(), r.publicacionId(), r.modoVenta() == null ? null : r.modoVenta().name());
    }
}
