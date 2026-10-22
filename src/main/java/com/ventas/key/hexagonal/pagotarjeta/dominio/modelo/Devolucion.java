package com.ventas.key.hexagonal.pagotarjeta.dominio.modelo;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Una devolucion de dinero de un cobro. Se RESERVA (pendiente) antes de hablar con la pasarela y se
 * CONFIRMA despues: si el back se cae entre las dos cosas, la reserva queda y se reintenta con la
 * misma clave, asi que ni se devuelve dos veces ni se pierde el rastro.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * @param clave clave de idempotencia (la misma que se manda a la pasarela); unica en la base
 */
public record Devolucion(Long id, Long intentoId, BigDecimal monto, String hechaPor, String clave,
                         boolean confirmada, Instant creadoEn) {

    public static Devolucion reservada(Long intentoId, BigDecimal monto, String hechaPor, String clave, Instant ahora) {
        return new Devolucion(null, intentoId, monto, hechaPor, clave, false, ahora);
    }

    public Devolucion conId(Long nuevoId) {
        return new Devolucion(nuevoId, intentoId, monto, hechaPor, clave, confirmada, creadoEn);
    }

    public Devolucion comoConfirmada() {
        return new Devolucion(id, intentoId, monto, hechaPor, clave, true, creadoEn);
    }
}
