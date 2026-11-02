package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

/**
 * Los clientes que compraron en un mes ({@code yyyy-MM}). {@code nuevos}: su primera compra fue ese mes; {@code recurrentes}:
 * ya habían comprado antes. Los totales son lo que compraron unos y otros.
 */
public record MesDeClientes(String mes, int nuevos, int recurrentes, double totalNuevos, double totalRecurrentes) {
}
