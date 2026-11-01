package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

/** Una forma de pago y qué parte de lo vendido representa. {@code porcentaje} va de 0 a 100 con un decimal. */
public record ParteDelTotal(String forma, int ventas, double total, double porcentaje, double comision) {
}
