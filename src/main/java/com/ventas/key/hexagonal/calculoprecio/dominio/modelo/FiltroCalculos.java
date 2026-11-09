package com.ventas.key.hexagonal.calculoprecio.dominio.modelo;

import java.time.LocalDate;

/** Filtros del listado de cálculos: parte del nombre del producto y rango de fechas del cálculo (los dos extremos incluidos). Todo opcional. */
public record FiltroCalculos(String nombre, LocalDate desde, LocalDate hasta) {
    public FiltroCalculos {
        nombre = nombre == null ? "" : nombre.trim();
    }
}
