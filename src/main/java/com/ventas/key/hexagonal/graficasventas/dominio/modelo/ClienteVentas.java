package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

/** Un cliente y lo que compró en el periodo. */
public record ClienteVentas(int clienteId, String nombre, int ventas, double total) {
}
