package com.ventas.key.hexagonal.precio.infraestructura.dto;

import com.ventas.key.hexagonal.precio.dominio.modelo.PreciosDeProducto;

/** El costo no viaja: la pantalla solo necesita saber si se esta vendiendo por debajo. */
public record PreciosProductoResponse(
        Integer productoId,
        double precioVenta,
        double precioRebaja,
        double precioACobrar,
        boolean vendeBajoCosto) {

    public static PreciosProductoResponse de(PreciosDeProducto p) {
        return new PreciosProductoResponse(p.productoId(), p.precioVenta(), p.precioRebaja(),
                p.precioACobrar(), p.vendeBajoCosto());
    }
}
