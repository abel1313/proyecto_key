package com.ventas.key.hexagonal.precio.infraestructura.dto;

import com.ventas.key.hexagonal.precio.dominio.modelo.PreciosDeArticulo;

/**
 * @param propio true si el articulo tiene precio propio; false si cobra el de su producto.
 *               El costo no viaja: la pantalla solo necesita saber si se vende por debajo.
 */
public record PreciosArticuloResponse(
        Integer varianteId,
        double precioVenta,
        double precioRebaja,
        boolean propio,
        boolean vendeBajoCosto) {

    public static PreciosArticuloResponse de(PreciosDeArticulo p) {
        return new PreciosArticuloResponse(p.varianteId(), p.precioVenta(), p.precioRebaja(),
                p.propio(), p.vendeBajoCosto());
    }
}
