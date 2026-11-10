package com.ventas.key.hexagonal.preciocantidad.dominio.modelo;

import com.ventas.key.hexagonal.preciocantidad.dominio.excepcion.GrupoDePrecioInvalidoException;

import java.math.BigDecimal;

/**
 * [Hexagonal: Domain] [Clean: Entity] Java puro.
 *
 * <p>Un producto del grupo y el precio al que se cobra cada pieza cuando el grupo junta su mínimo. {@code nombre} es solo para mostrar.
 */
public record MiembroDeGrupo(int productoId, String nombre, BigDecimal precioPromocion) {

    public MiembroDeGrupo {
        if (precioPromocion == null || precioPromocion.signum() <= 0) {
            throw new GrupoDePrecioInvalidoException("El precio de promoción de " + (nombre == null ? "el producto " + productoId : nombre)
                    + " debe ser mayor a cero.");
        }
        precioPromocion = precioPromocion.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
