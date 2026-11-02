package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

import java.time.LocalDate;
import java.util.List;

/**
 * Cuándo se vende: por día de la semana y hora. {@code celdas} trae solo las combinaciones con ventas; {@code porDia} siempre trae
 * los 7 días. {@code mejorDia} (1–7) y {@code mejorHora} (0–23) son los de más dinero, o {@code null} si no hubo ventas.
 */
public record MapaDeVentas(LocalDate desde, LocalDate hasta, int ventas, double total, List<CeldaDeCalor> celdas,
                           List<TotalDeDia> porDia, Integer mejorDia, Integer mejorHora) {
    public MapaDeVentas {
        celdas = List.copyOf(celdas);
        porDia = List.copyOf(porDia);
    }
}
