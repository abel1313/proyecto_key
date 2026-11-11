package com.ventas.key.hexagonal.flete.dominio.modelo;

/** [Hexagonal: Domain] Una línea de venta recién creada: su id, el producto y las piezas vendidas. */
public record LineaVendida(int detalleVentaId, int productoId, int cantidad) {
}
