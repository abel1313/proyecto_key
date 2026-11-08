package com.ventas.key.hexagonal.campana.dominio.modelo;

import com.ventas.key.hexagonal.campana.dominio.excepcion.CampanaInvalidaException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * [Hexagonal: Domain] [Clean: Entity] Java puro. Un producto de la campaña y el precio al que se cobra cada pieza a quien es recurrente.
 * {@code nombre} es solo para mostrar.
 */
public record PrecioDeCampana(int productoId, String nombre, BigDecimal precio) {

    public PrecioDeCampana {
        if (precio == null || precio.signum() <= 0) {
            throw new CampanaInvalidaException("El precio de campaña de " + (nombre == null ? "el producto " + productoId : "«" + nombre + "»")
                    + " debe ser mayor a cero.");
        }
        precio = precio.setScale(2, RoundingMode.HALF_UP);
    }
}
