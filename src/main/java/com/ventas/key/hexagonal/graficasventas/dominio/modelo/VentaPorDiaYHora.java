package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

import java.time.LocalDate;

/** Lo vendido en una fecha y una hora (0–23, hora local), tal como lo entrega el puerto. */
public record VentaPorDiaYHora(LocalDate dia, int hora, int ventas, double total) {
}
