package com.ventas.key.hexagonal.preciocantidad.dominio.modelo;

import java.math.BigDecimal;

/**
 * [Hexagonal: Domain] Una línea de un pedido ya guardado, vista solo para volver a calcular su precio por cantidad.
 *
 * @param precioActual   lo que se cobra hoy por pieza
 * @param precioSinGrupo el precio que tenía antes de cobrarse con precio de grupo, o {@code null} si la línea no se cobró con precio de grupo
 * @param esDePromocion  las líneas de un combo no entran al precio por cantidad
 */
public record LineaDePrecio(int detalleId, int productoId, BigDecimal precioActual, BigDecimal precioSinGrupo, boolean esDePromocion) {

    /** El precio al que se cobraría la línea si no hubiera grupo: el que tenía antes, o el actual si nunca se cobró con grupo. */
    public BigDecimal precioBase() {
        return precioSinGrupo != null ? precioSinGrupo : precioActual;
    }
}
