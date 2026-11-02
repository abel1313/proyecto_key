package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

/** Lo vendido, ganado y gastado en un tramo de días (ventas devueltas no cuentan). {@code gananciaNeta} = ganancia menos gastos. */
public record PeriodoDeVentas(String desde, String hasta, int numeroVentas, double ventas, double ganancia, double gastos,
                              double gananciaNeta) {
}
