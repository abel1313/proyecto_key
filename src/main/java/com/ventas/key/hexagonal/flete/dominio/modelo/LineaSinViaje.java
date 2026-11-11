package com.ventas.key.hexagonal.flete.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** [Hexagonal: Domain] Una línea de venta de un producto con piezas que todavía no se ligan a ningún viaje de compra ({@code pendientes} de {@code cantidad}). */
public record LineaSinViaje(int detalleVentaId, int ventaId, LocalDateTime fecha, int cantidad, int pendientes, BigDecimal precioUnitario) {
}
