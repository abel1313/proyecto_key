package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

import java.time.LocalDate;

/** Un Apartado o Ir pagando con saldo por cobrar, tal como lo entrega el puerto. {@code tipo}: APARTADO o FIADO. */
public record SaldoPendiente(int pedidoId, String cliente, String tipo, double saldo, LocalDate fechaPedido) {
}
