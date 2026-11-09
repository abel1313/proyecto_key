package com.ventas.key.hexagonal.calculoprecio.infraestructura.dto;

import com.ventas.key.hexagonal.calculoprecio.dominio.modelo.CalculoDeProducto;

import java.math.BigDecimal;

/** El último cálculo de un producto con su nombre y el precio de venta de hoy; {@code precioCambio} = el precio de hoy ya no es el que se cobró en ese cálculo. */
public record CalculoDeProductoResponse(CalculoPrecioResponse calculo, String nombreProducto, BigDecimal precioVentaActual, boolean precioCambio) {
    public static CalculoDeProductoResponse de(CalculoDeProducto c) {
        return new CalculoDeProductoResponse(CalculoPrecioResponse.de(c.calculo()), c.nombreProducto(), c.precioVentaActual(), c.precioCambio());
    }
}
