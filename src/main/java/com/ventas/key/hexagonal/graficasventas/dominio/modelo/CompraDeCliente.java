package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

import java.time.YearMonth;

/** Lo que un cliente registrado compró en un mes (ventas devueltas y ventas a su propio perfil de vendedor no cuentan). */
public record CompraDeCliente(int clienteId, YearMonth mes, int ventas, double total) {
}
