package com.ventas.key.hexagonal.flete.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** [Hexagonal: Domain] Piezas de una línea de venta que salieron de un producto del viaje (las ventas devueltas no se incluyen). */
public record VentaLigada(int ventaId, int detalleVentaId, LocalDateTime fechaVenta, int productoId, int cantidad, BigDecimal fletePorPieza,
                          BigDecimal totalVendido, BigDecimal costo, BigDecimal ganancia) {
}
