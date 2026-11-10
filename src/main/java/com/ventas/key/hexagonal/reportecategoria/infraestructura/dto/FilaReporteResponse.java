package com.ventas.key.hexagonal.reportecategoria.infraestructura.dto;

import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.FilaBase;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.FilaReporte;

import java.math.BigDecimal;

/** Un producto del reporte, plano para el front. «categoria» null = «Otros» (sin categoría). */
public record FilaReporteResponse(int productoId, String nombre, String codigoBarras, String categoria,
                                  BigDecimal precioCompra, BigDecimal precioVenta, BigDecimal precioRebaja, BigDecimal precioRebajaCampana,
                                  long piezasVendidas, long ventas, BigDecimal totalVendido, BigDecimal costoVendido, BigDecimal gananciaVendida,
                                  BigDecimal porcentajeCalculado, boolean calculoNuevo, BigDecimal porcentajeReal,
                                  BigDecimal fletePorPieza, boolean sinFlete,
                                  int porVender, BigDecimal gananciaPorVender, BigDecimal gananciaPorVenderRebaja, BigDecimal gananciaPorVenderCampana,
                                  BigDecimal gananciaTotal, BigDecimal gananciaTotalRebaja, BigDecimal gananciaTotalCampana,
                                  BigDecimal fleteEnLoVendido, BigDecimal fleteEnStock) {

    public static FilaReporteResponse de(FilaReporte f) {
        FilaBase b = f.base();
        return new FilaReporteResponse(b.productoId(), b.nombre(), b.codigoBarras(), b.categoria(),
                b.precioCompra(), b.precioVenta(), b.precioRebaja(), b.precioCampana(),
                b.piezas(), b.ventas(), b.total(), b.costo(), b.ganancia(),
                f.margenCalculadoPct(), f.calculoNuevo(), f.margenRealPct(),
                f.flete(), f.sinFlete(),
                Math.max(0, b.stock()), f.gananciaPorVender(), f.gananciaPorVenderRebaja(), f.gananciaPorVenderCampana(),
                b.ganancia().add(f.gananciaPorVender()), b.ganancia().add(f.gananciaPorVenderRebaja()), b.ganancia().add(f.gananciaPorVenderCampana()),
                com.ventas.key.hexagonal.reportecategoria.dominio.modelo.ReglasReporte.fleteDeLoVendido(f),
                com.ventas.key.hexagonal.reportecategoria.dominio.modelo.ReglasReporte.fleteEnStock(f));
    }
}
