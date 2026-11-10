package com.ventas.key.hexagonal.reportecategoria.dominio.modelo;

import java.math.BigDecimal;

/**
 * Un producto tal como sale de la base: sus precios de hoy y lo que se vendió en las fechas (ya sin ventas devueltas).
 * Sin ventas: piezas, ventas, total, costo y ganancia en cero. {@code precioCampana} es el de la campaña más reciente que lo trae.
 */
public record FilaBase(int productoId, String nombre, String codigoBarras, String categoria,
                       BigDecimal precioCompra, BigDecimal precioVenta, BigDecimal precioRebaja, BigDecimal precioCampana,
                       int stock, long piezas, long ventas, BigDecimal total, BigDecimal costo, BigDecimal ganancia) {
}
