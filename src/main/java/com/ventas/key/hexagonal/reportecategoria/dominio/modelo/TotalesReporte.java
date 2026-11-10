package com.ventas.key.hexagonal.reportecategoria.dominio.modelo;

import java.math.BigDecimal;

/** Totales de cada columna, de TODAS las filas del reporte (no solo de la página que se ve). */
public record TotalesReporte(int productos, int productosVendidos, int productosSinVender,
                             long piezas, BigDecimal total, BigDecimal costo, BigDecimal gananciaVendida, BigDecimal margenRealPct,
                             long piezasPorVender, BigDecimal gananciaPorVender, BigDecimal gananciaTotal) {
}
