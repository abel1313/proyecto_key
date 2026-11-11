package com.ventas.key.hexagonal.flete.dominio.modelo;

import java.math.BigDecimal;

/**
 * [Hexagonal: Domain] Un producto listo para entrar a un viaje: su costo de hoy y el flete por pieza que ya tiene guardado en su último cálculo de
 * precio ({@code transporte} en cero = sin flete guardado).
 */
public record ProductoConFlete(int productoId, String nombre, String codigoBarras, BigDecimal precioCosto, int stock, BigDecimal transporte) {

    public boolean tieneFleteGuardado() {
        return transporte != null && transporte.signum() > 0;
    }
}
