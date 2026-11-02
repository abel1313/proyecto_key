package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

/** Lo vendido en un día de la semana a cierta hora, sumando todas las semanas del periodo. {@code diaSemana}: 1 = lunes … 7 = domingo. */
public record CeldaDeCalor(int diaSemana, int hora, int ventas, double total) {
}
