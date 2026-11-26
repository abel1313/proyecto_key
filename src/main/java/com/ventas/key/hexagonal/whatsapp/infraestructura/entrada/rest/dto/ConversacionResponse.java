package com.ventas.key.hexagonal.whatsapp.infraestructura.entrada.rest.dto;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ClienteWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ResumenDeConversacion;

import java.time.Instant;

/**
 * Una conversación en la lista de la pantalla. Todas las fechas van en UTC, formato ISO-8601 con "Z"
 * (ej. "2026-11-26T22:00:00Z"): el front las convierte a su hora local.
 */
public record ConversacionResponse(long id, String telefono, String nombre, String ultimoMensaje,
                                   String ultimaActividad, String ultimoMensajeDelCliente,
                                   boolean ventanaAbierta, String ventanaCierraEn, boolean botPausado,
                                   boolean requiereAtencion, long sinResponder) {

    public static ConversacionResponse de(ResumenDeConversacion r) {
        ClienteWhatsapp c = r.cliente();
        return new ConversacionResponse(c.id(), c.telefono(), c.nombre(), c.ultimoMensaje(), iso(c.ultimaActividadAt()),
                iso(c.ultimoEntranteAt()), r.ventanaAbierta(), iso(r.ventanaCierraEn()), r.botPausado(),
                c.requiereAtencion(), r.sinResponder());
    }

    static String iso(Instant i) {
        return i == null ? null : i.toString();
    }
}
