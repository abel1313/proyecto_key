package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

/** Cuánto hay por cobrar de pedidos con cierta antigüedad. */
public record TramoPorCobrar(String etiqueta, int pedidos, double saldo) {
}
