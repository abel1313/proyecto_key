package com.ventas.key.hexagonal.flete.dominio.modelo;

import java.math.BigDecimal;

/** [Hexagonal: Domain] Cuántas piezas de una línea de venta salen de un renglón de viaje, y a cuánto flete por pieza. */
public record ParteAsignada(int renglonId, int cantidad, BigDecimal fletePorPieza) {
}
