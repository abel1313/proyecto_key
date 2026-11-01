package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

import java.time.LocalDate;
import java.util.List;

/**
 * Cómo te pagaron en un periodo. {@code formas} viene de la que más vendió a la que menos.
 * {@code comisionTotal} es lo que se quedaron las terminales en ese periodo.
 */
public record VentasPorFormaDePago(LocalDate desde, LocalDate hasta, double total, double comisionTotal,
                                   List<ParteDelTotal> formas) {
    public VentasPorFormaDePago {
        formas = List.copyOf(formas);
    }
}
