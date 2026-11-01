package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

/** Lo vendido en un mes, tal como lo entrega el puerto (ventas devueltas no cuentan). */
public record VentasDelMes(int numeroVentas, double ventas, double ganancia) {
}
