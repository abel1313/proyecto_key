package com.ventas.key.hexagonal.reportecategoria.dominio.modelo;

import java.math.BigDecimal;

/** Cálculo que el reporte guarda para un producto que no tenía ninguno (sin flete). */
public record NuevoCalculo(int productoId, BigDecimal costo, BigDecimal precioVenta, BigDecimal margenPct) {
}
