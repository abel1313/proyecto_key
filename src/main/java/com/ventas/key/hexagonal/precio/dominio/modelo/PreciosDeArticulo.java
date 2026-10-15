package com.ventas.key.hexagonal.precio.dominio.modelo;

import com.ventas.key.hexagonal.precio.dominio.excepcion.PrecioInvalidoException;

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
 * @param precioRebaja su descuento, 0 = sin. Se cobra si el admin lo elige, o siempre con usarDescuento
 * @param propio       true si el articulo tiene precio propio; false si hereda el del producto
 * @param usarDescuento R8: el admin activo "Precio descuento"; el articulo se vende al descuento
 */
public record PreciosDeArticulo(
        Integer varianteId,
        String nombre,
        double precioCosto,
        double precioVenta,
        double precioRebaja,
        boolean propio,
        boolean usarDescuento) {

    /**
     * Precio propio para este articulo, validado con las mismas reglas que el del producto. Con
     * {@code usar} el descuento pasa a ser el precio del articulo (R8), asi que tiene que existir
     * y ser menor al normal.
     */
    public PreciosDeArticulo conPrecios(Double nuevoVenta, Double nuevoRebaja, boolean usar) {
        double rebaja = ReglasDePrecio.descuentoValido(nuevoVenta, nuevoRebaja);
        if (usar && !(rebaja > 0 && rebaja < nuevoVenta)) {
            throw PrecioInvalidoException.descuentoParaUsar(nuevoVenta);
        }
        return new PreciosDeArticulo(varianteId, nombre, precioCosto, nuevoVenta, rebaja, true, usar);
    }

    /** Deja de tener precio propio y vuelve al del producto (R6). */
    public PreciosDeArticulo heredando() {
        return new PreciosDeArticulo(varianteId, nombre, precioCosto, precioVenta, precioRebaja, false, false);
    }

    /** El descuento que de verdad se puede cobrar: mayor a 0 y menor al normal; si no, 0. */
    public double descuentoCobrable() {
        return precioRebaja > 0 && precioRebaja < precioVenta ? precioRebaja : 0.0;
    }

    /** Al que se vende por default: el descuento si esta activo (R8), si no el normal. */
    public double precioACobrar() {
        return usarDescuento && precioRebaja > 0 && precioRebaja < precioVenta ? precioRebaja : precioVenta;
    }

    /** R3: se permite, pero la pantalla avisa. */
    public boolean vendeBajoCosto() {
        return precioCosto > 0 && ReglasDePrecio.minimoCobrable(precioVenta, precioRebaja) < precioCosto;
    }
}
