package com.ventas.key.hexagonal.busquedapedido.dominio.modelo;

import java.time.LocalDate;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Atajos por la fecha de entrega o recogida (R7). Solo cuentan los pedidos que todavia esperan
 * entrega: los Entregados, Pagados y Cancelados ya no (mismo criterio que el "⚠ Atrasado" de la
 * card de Mis pedidos).
 */
public enum CuandoSeEntrega {

    HOY,
    MANANA,
    /** Hoy y los 6 dias siguientes. */
    ESTA_SEMANA,
    /** La fecha ya paso y no se ha entregado. */
    ATRASADOS;

    /** Los dias que abarca, contados desde {@code hoy}. Atrasados = todo lo anterior a hoy. */
    public RangoDeFechas rango(LocalDate hoy) {
        return switch (this) {
            case HOY -> new RangoDeFechas(hoy, hoy);
            case MANANA -> new RangoDeFechas(hoy.plusDays(1), hoy.plusDays(1));
            case ESTA_SEMANA -> new RangoDeFechas(hoy, hoy.plusDays(6));
            case ATRASADOS -> new RangoDeFechas(null, hoy.minusDays(1));
        };
    }
}
