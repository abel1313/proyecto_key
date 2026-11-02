package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

/** Pedidos (no cancelados) de un lugar de entrega, tal como los entrega el puerto. */
public record ZonaDeEntrega(String zona, boolean recogerEnTienda, int pedidos, double total) {
}
