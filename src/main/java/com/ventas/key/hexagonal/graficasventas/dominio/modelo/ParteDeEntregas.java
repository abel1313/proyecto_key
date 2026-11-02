package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

/** Un lugar de entrega y qué parte de los pedidos representa ({@code porcentaje} de 0 a 100, un decimal). */
public record ParteDeEntregas(String zona, boolean recogerEnTienda, int pedidos, double total, double porcentaje) {
}
