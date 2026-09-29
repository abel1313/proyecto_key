package com.ventas.key.hexagonal.precio.dominio.modelo;

/**
 * Los precios de un articulo (una talla/color). Si no tiene propios, son los de su producto.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Nace del hotfix del 2026-09-29: el boton 💲 cambiaba el precio del producto y con eso el de
 * todas sus tallas. El dueño lo usa para hacerle precio a un cliente que se lleva mucho, y eso
 * es de UN articulo, no de todos (R5 del README).
 *
 * @param precioCosto  el del producto; el articulo no tiene costo propio
 * @param precioVenta  el normal de este articulo
 * @param precioRebaja su descuento, 0 = sin. Solo se cobra si el admin lo elige
 * @param propio       true si el articulo tiene precio propio; false si hereda el del producto
 */
public record PreciosDeArticulo(
        Integer varianteId,
        String nombre,
        double precioCosto,
        double precioVenta,
        double precioRebaja,
        boolean propio) {

    /** Precio propio para este articulo, validado con las mismas reglas que el del producto. */
    public PreciosDeArticulo conPrecios(Double nuevoVenta, Double nuevoRebaja) {
        double rebaja = ReglasDePrecio.descuentoValido(nuevoVenta, nuevoRebaja);
        return new PreciosDeArticulo(varianteId, nombre, precioCosto, nuevoVenta, rebaja, true);
    }

    /** Deja de tener precio propio y vuelve al del producto (R6). */
    public PreciosDeArticulo heredando() {
        return new PreciosDeArticulo(varianteId, nombre, precioCosto, precioVenta, precioRebaja, false);
    }

    /** R3: se permite, pero la pantalla avisa. */
    public boolean vendeBajoCosto() {
        return precioCosto > 0 && ReglasDePrecio.minimoCobrable(precioVenta, precioRebaja) < precioCosto;
    }
}
