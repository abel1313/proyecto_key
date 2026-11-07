package com.ventas.key.hexagonal.preciocantidad.dominio.modelo;

/** [Hexagonal: Domain] Una línea de la venta: cuántas piezas de un producto (sin importar el artículo: talla, color). */
public record LineaPorCantidad(int productoId, int cantidad) {
}
