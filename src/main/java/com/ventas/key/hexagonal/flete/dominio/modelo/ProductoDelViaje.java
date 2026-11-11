package com.ventas.key.hexagonal.flete.dominio.modelo;

import com.ventas.key.hexagonal.flete.dominio.excepcion.ViajeInvalidoException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * [Hexagonal: Domain] Un producto que llegó en el viaje: cuántas piezas, a cuánto salió cada una y cuánto flete lleva cada pieza.
 * {@code nombre} y {@code codigoBarras} son solo para mostrar.
 */
public record ProductoDelViaje(int productoId, String nombre, String codigoBarras, int piezas, BigDecimal costoUnitario, BigDecimal fletePorPieza) {

    public ProductoDelViaje(int productoId, int piezas, BigDecimal costoUnitario, BigDecimal fletePorPieza) {
        this(productoId, null, null, piezas, costoUnitario, fletePorPieza);
    }

    public ProductoDelViaje {
        String quien = nombre == null ? "el producto " + productoId : "«" + nombre + "»";
        if (productoId <= 0) {
            throw new ViajeInvalidoException("Falta elegir el producto de uno de los renglones del viaje.");
        }
        if (piezas <= 0) {
            throw new ViajeInvalidoException("Las piezas de " + quien + " deben ser al menos 1.");
        }
        if (piezas > 100_000) {
            throw new ViajeInvalidoException("Las piezas de " + quien + " son demasiadas.");
        }
        costoUnitario = costoUnitario == null ? BigDecimal.ZERO.setScale(2) : costoUnitario.setScale(2, RoundingMode.HALF_UP);
        fletePorPieza = fletePorPieza == null ? BigDecimal.ZERO.setScale(2) : fletePorPieza.setScale(2, RoundingMode.HALF_UP);
        if (costoUnitario.signum() < 0) {
            throw new ViajeInvalidoException("El costo de " + quien + " no puede ser negativo.");
        }
        if (fletePorPieza.signum() < 0) {
            throw new ViajeInvalidoException("El flete por pieza de " + quien + " no puede ser negativo.");
        }
        if (costoUnitario.compareTo(ViajeDeCompra.MONTO_MAXIMO) > 0 || fletePorPieza.compareTo(ViajeDeCompra.MONTO_MAXIMO) > 0) {
            throw new ViajeInvalidoException("El costo o el flete de " + quien + " es demasiado grande.");
        }
    }

    /** El flete que lleva todo el renglón: flete por pieza × piezas. */
    public BigDecimal fleteDelRenglon() {
        return fletePorPieza.multiply(BigDecimal.valueOf(piezas)).setScale(2, RoundingMode.HALF_UP);
    }

    /** Lo que costaron las piezas de este renglón sin flete: costo × piezas. */
    public BigDecimal compraDelRenglon() {
        return costoUnitario.multiply(BigDecimal.valueOf(piezas)).setScale(2, RoundingMode.HALF_UP);
    }
}
