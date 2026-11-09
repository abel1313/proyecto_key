package com.ventas.key.hexagonal.calculoprecio.dominio.modelo;

import java.math.BigDecimal;

/** [Clean: Entity] El último cálculo de precio de un producto, junto con su nombre y el precio de venta que tiene hoy (para ver si todavía coincide). */
public record CalculoDeProducto(CalculoPrecio calculo, String nombreProducto, BigDecimal precioVentaActual) {

    /** El precio de venta de hoy ya no es el que se cobró en el último cálculo (se cambió el precio después). */
    public boolean precioCambio() {
        return precioVentaActual != null && precioVentaActual.compareTo(calculo.precioCobrado()) != 0;
    }
}
