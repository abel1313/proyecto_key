package com.ventas.key.hexagonal.reportecategoria.dominio.modelo;

import java.math.BigDecimal;

/**
 * Un producto tal como sale de la base: sus precios de hoy y lo que se vendió en las fechas (ya sin ventas devueltas).
 * Sin ventas: piezas, ventas, total, costo y ganancia en cero. Lo vendido también viene repartido por TIPO DE PRECIO (venta, rebaja, campaña u otro:
 * grupo, promoción...) en piezas y en dinero; {@code piezasEstimadas} son las piezas de ventas anteriores a la migración del tipo de precio (se
 * clasificaron comparando el precio cobrado con los precios del producto). {@code precioCampana} es el de la campaña más reciente que lo trae.
 */
public record FilaBase(int productoId, String nombre, String codigoBarras, String categoria,
                       BigDecimal precioCompra, BigDecimal precioVenta, BigDecimal precioRebaja, BigDecimal precioCampana,
                       int stock, long piezas, long ventas, BigDecimal total, BigDecimal costo, BigDecimal ganancia,
                       long piezasVenta, long piezasRebaja, long piezasCampana, long piezasOtro,
                       BigDecimal montoVenta, BigDecimal montoRebaja, BigDecimal montoCampana, BigDecimal montoOtro, long piezasEstimadas,
                       BigDecimal fleteReal, long piezasConViaje) {
}
