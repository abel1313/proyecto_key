package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

/**
 * Un mes de la gráfica de ventas, gastos y ganancia.
 *
 * @param mes           {@code yyyy-MM}
 * @param gananciaNeta  ganancia menos gastos (la misma cuenta que la tarjeta «Ganancia neta» del Dashboard)
 */
public record PuntoMensual(String mes, int numeroVentas, double ventas, double ganancia, double gastos, double gananciaNeta) {
}
