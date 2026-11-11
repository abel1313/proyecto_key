package com.ventas.key.hexagonal.reportecategoria.dominio.modelo;

import java.math.BigDecimal;

/** Totales de cada columna, de TODAS las filas del reporte (no solo de la página que se ve). */
public record TotalesReporte(int productos, int productosVendidos, int productosSinVender,
                             long piezas, BigDecimal total, BigDecimal costo, BigDecimal gananciaVendida, BigDecimal margenRealPct,
                             long piezasPorVender, BigDecimal gananciaPorVender, BigDecimal gananciaTotal,
                             BigDecimal gananciaPorVenderRebaja, BigDecimal gananciaPorVenderCampana,
                             BigDecimal gananciaTotalRebaja, BigDecimal gananciaTotalCampana,
                             BigDecimal fleteVendido, BigDecimal fleteEnStock,
                             BigDecimal sumaCompra, BigDecimal sumaVenta, BigDecimal sumaRebaja, BigDecimal sumaCampana,
                             long piezasVenta, long piezasRebaja, long piezasCampana, long piezasOtro,
                             BigDecimal montoVenta, BigDecimal montoRebaja, BigDecimal montoCampana, BigDecimal montoOtro, long piezasEstimadas,
                             BigDecimal inversion) {
}
