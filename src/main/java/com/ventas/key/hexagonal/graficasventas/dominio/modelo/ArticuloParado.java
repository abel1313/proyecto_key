package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

import java.time.LocalDate;

/** Un artículo con piezas en bodega, tal como lo entrega el puerto. {@code ultimaVenta} null = nunca se ha vendido. */
public record ArticuloParado(int articuloId, String nombre, int stock, double costo, LocalDate ultimaVenta) {
}
