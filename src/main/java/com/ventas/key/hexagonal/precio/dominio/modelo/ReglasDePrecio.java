package com.ventas.key.hexagonal.precio.dominio.modelo;

import com.ventas.key.hexagonal.precio.dominio.excepcion.PrecioInvalidoException;

/**
 * R1 y R2 del README, las mismas para el precio de un producto y el de un articulo.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 */
final class ReglasDePrecio {

    private ReglasDePrecio() {
    }

    /** Valida el par y devuelve el descuento normalizado (null = 0 = sin descuento). */
    static double descuentoValido(Double venta, Double rebaja) {
        if (venta == null || venta <= 0) {
            throw PrecioInvalidoException.ventaObligatoria();
        }
        double descuento = rebaja == null ? 0.0 : rebaja;
        if (descuento < 0) {
            throw PrecioInvalidoException.rebajaNegativa();
        }
        if (descuento > venta) {
            throw PrecioInvalidoException.rebajaMayorQueVenta(descuento, venta);
        }
        return descuento;
    }

    /** Lo mas barato a lo que se puede llegar a cobrar: el descuento si hay, si no el normal. */
    static double minimoCobrable(double venta, double rebaja) {
        return rebaja > 0 ? rebaja : venta;
    }
}
