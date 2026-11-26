package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import java.time.Instant;

/** Aviso de Meta sobre un mensaje nuestro: llegó, se leyó o falló (con su código de error, ej. 131047). */
public record EstadoDeEntrega(String waMessageId, EstadoMensaje estado, Integer codigoError, String motivo, Instant en) {
}
