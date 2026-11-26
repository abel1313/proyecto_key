package com.ventas.key.hexagonal.whatsapp.infraestructura.entrada.rest.dto;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MensajeWhatsapp;

/**
 * Un mensaje del historial. {@code direccion}: ENTRANTE | SALIENTE. {@code origen}: CLIENTE | BOT | PERSONA | SISTEMA.
 * {@code estado}: RECIBIDO | ENVIADO | ENTREGADO | LEIDO | FALLIDO | BLOQUEADO_VENTANA | BLOQUEADO_SIN_PAGO |
 * PENDIENTE_PLANTILLA. {@code motivo} y {@code codigoError} explican por qué no salió o falló. {@code fecha} en UTC (ISO-8601).
 */
public record MensajeResponse(long id, String direccion, String origen, String tipo, String contenido, String estado,
                              String motivo, Integer codigoError, String plantilla, String fecha) {

    public static MensajeResponse de(MensajeWhatsapp m) {
        return new MensajeResponse(m.id(), m.direccion().name(), m.origen().name(), m.tipo(), m.contenido(),
                m.estado().name(), m.motivo(), m.codigoError(), m.plantilla(),
                m.enviadoEn() == null ? null : m.enviadoEn().toString());
    }
}
