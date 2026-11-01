package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

import java.time.LocalDate;

/** Un artículo parado con lo que cuesta lo que tiene guardado. {@code diasSinVender} null = nunca se ha vendido. */
public record CasoCapitalParado(int articuloId, String nombre, int stock, double costo, double capital,
                                LocalDate ultimaVenta, Long diasSinVender) {
}
