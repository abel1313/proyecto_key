package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

/** Lo vendido en un día de la semana (1 = lunes … 7 = domingo), con su nombre en palabras del dueño. */
public record TotalDeDia(int diaSemana, String nombre, int ventas, double total) {
}
