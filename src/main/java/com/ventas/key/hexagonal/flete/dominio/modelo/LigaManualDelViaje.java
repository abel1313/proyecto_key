package com.ventas.key.hexagonal.flete.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** [Hexagonal: Domain] Una liga hecha a mano: piezas de una línea de venta anterior que se ligaron a un producto del viaje (se puede deshacer). */
public record LigaManualDelViaje(int ligaId, int ventaId, int detalleVentaId, LocalDateTime fechaVenta, int productoId, String producto,
                                 int cantidad, BigDecimal fletePorPieza) {
}
