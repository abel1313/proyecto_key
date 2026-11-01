package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

/** Un pedido con saldo, con sus días de antigüedad y cómo se le llama al tipo en pantalla («Apartado» o «Ir pagando»). */
public record CasoPorCobrar(int pedidoId, String cliente, String tipo, double saldo, long dias) {
}
