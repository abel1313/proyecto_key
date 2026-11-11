package com.ventas.key.hexagonal.flete.dominio.modelo;

/** [Hexagonal: Domain] Lo que el administrador liga a mano: cuántas piezas de una línea de venta salieron del viaje elegido. */
public record LigaManual(int detalleVentaId, int cantidad) {
}
