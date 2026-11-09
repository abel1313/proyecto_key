package com.ventas.key.hexagonal.calculoprecio.infraestructura.dto;

import java.math.BigDecimal;

/** Lo que manda el front al pulsar «Usar este precio» (y luego guardar el producto). Ganancia y margen real no se mandan: se calculan en el back. */
public record GuardarCalculoPrecioRequest(Integer productoId, BigDecimal costoCompra, BigDecimal transporte, BigDecimal otros,
                                          BigDecimal comisionPct, String comisionDescripcion, BigDecimal margenPct,
                                          BigDecimal precioCalculado, BigDecimal precioCobrado, Boolean actualizoPrecioCompra, String nota) { }
