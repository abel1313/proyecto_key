package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import java.time.Instant;

/**
 * Una línea del historial. {@code motivo} y {@code codigoError} explican por qué no salió o falló
 * (ej. 131047). {@code enviadoEn} es la hora de Meta para los entrantes y la nuestra para los salientes.
 */
public record MensajeWhatsapp(long id, long clienteId, String waMessageId, DireccionMensaje direccion,
                              OrigenMensaje origen, String tipo, String contenido, EstadoMensaje estado,
                              String motivo, Integer codigoError, String plantilla, Instant enviadoEn) {
}
