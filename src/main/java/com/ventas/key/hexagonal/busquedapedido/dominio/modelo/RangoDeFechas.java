package com.ventas.key.hexagonal.busquedapedido.dominio.modelo;

import com.ventas.key.hexagonal.busquedapedido.dominio.excepcion.FiltroInvalidoException;

import java.time.LocalDate;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Un rango de dias, con los dos extremos incluidos. Cualquiera de los dos puede faltar
 * ("desde el 1 de octubre", "hasta hoy"), pero si estan los dos, desde no puede ir despues de
 * hasta (R6).
 */
public record RangoDeFechas(LocalDate desde, LocalDate hasta) {

    public RangoDeFechas {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new FiltroInvalidoException(
                    "La fecha \"desde\" (" + desde + ") va despues de la fecha \"hasta\" (" + hasta + ")");
        }
    }

    public static RangoDeFechas de(LocalDate desde, LocalDate hasta) {
        return desde == null && hasta == null ? null : new RangoDeFechas(desde, hasta);
    }
}
