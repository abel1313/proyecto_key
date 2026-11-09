package com.ventas.key.hexagonal.calculoprecio.infraestructura.dto;

import com.ventas.key.hexagonal.calculoprecio.dominio.modelo.CalculoPrecio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Un cálculo de precio con lo que el front necesita mostrar. */
public record CalculoPrecioResponse(Integer id, int productoId, BigDecimal costoCompra, BigDecimal transporte, BigDecimal otros,
                                    BigDecimal costoTotal, BigDecimal comisionPct, String comisionDescripcion, BigDecimal margenPct,
                                    BigDecimal precioCalculado, BigDecimal precioCobrado, BigDecimal gananciaReal, BigDecimal margenReal,
                                    boolean actualizoPrecioCompra, String nota, String usuario, LocalDateTime fecha) {
    public static CalculoPrecioResponse de(CalculoPrecio c) {
        return new CalculoPrecioResponse(c.id(), c.productoId(), c.costoCompra(), c.transporte(), c.otros(), c.costoTotal(), c.comisionPct(),
                c.comisionDescripcion(), c.margenPct(), c.precioCalculado(), c.precioCobrado(), c.gananciaReal(), c.margenReal(),
                c.actualizoPrecioCompra(), c.nota(), c.usuario(), c.fecha());
    }
}
