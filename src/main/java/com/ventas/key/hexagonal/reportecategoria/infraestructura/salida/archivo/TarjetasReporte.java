package com.ventas.key.hexagonal.reportecategoria.infraestructura.salida.archivo;

import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.TipoReporte;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.TotalesReporte;

import java.math.BigDecimal;
import java.util.List;

/** Las tarjetas con los totales de arriba del Excel y del PDF, según el tipo de reporte (siempre 8, para que el diseño no cambie). */
final class TarjetasReporte {

    record Tarjeta(String etiqueta, BigDecimal valor, boolean dinero, String sub, boolean verde) { }

    private TarjetasReporte() { }

    static List<Tarjeta> de(TipoReporte tipo, TotalesReporte t) {
        BigDecimal productos = BigDecimal.valueOf(t.productos());
        BigDecimal piezas = BigDecimal.valueOf(t.piezas());
        String vendidos = t.productosVendidos() + " vendidos · " + t.productosSinVender() + " sin vender";
        String margen = t.margenRealPct() == null ? "" : String.format(java.util.Locale.US, "%.2f%% real", t.margenRealPct());
        if (tipo == TipoReporte.VENTAS) {
            return List.of(
                    new Tarjeta("PRODUCTOS", productos, false, vendidos, false),
                    new Tarjeta("PIEZAS VENDIDAS", piezas, false, t.piezasEstimadas() + " piezas con tipo de precio estimado", false),
                    new Tarjeta("TOTAL VENDIDO", t.total(), true, "Costo " + dinero(t.costo()), false),
                    new Tarjeta("GANANCIA VENDIDA", t.gananciaVendida(), true, margen, true),
                    new Tarjeta("A PRECIO DE VENTA", t.montoVenta(), true, t.piezasVenta() + " piezas", false),
                    new Tarjeta("A REBAJA", t.montoRebaja(), true, t.piezasRebaja() + " piezas", false),
                    new Tarjeta("A CAMPAÑA", t.montoCampana(), true, t.piezasCampana() + " piezas", false),
                    new Tarjeta("A OTRO PRECIO", t.montoOtro(), true, t.piezasOtro() + " piezas (grupo, promoción...)", false));
        }
        return List.of(
                new Tarjeta("PRODUCTOS", productos, false, vendidos, false),
                new Tarjeta("PIEZAS VENDIDAS", piezas, false, t.piezasPorVender() + " en stock por vender", false),
                new Tarjeta("TOTAL VENDIDO", t.total(), true, "Costo " + dinero(t.costo()), false),
                new Tarjeta("GANANCIA VENDIDA", t.gananciaVendida(), true, margen, true),
                new Tarjeta("FLETE LLEVAMOS", t.fleteVendido(), true, "En stock: " + dinero(t.fleteEnStock()), false),
                new Tarjeta("TOTAL CON PRECIO VENTA", t.gananciaTotal(), true, "Vendida + por vender " + dinero(t.gananciaPorVender()), true),
                new Tarjeta("TOTAL CON REBAJA", t.gananciaTotalRebaja(), true, "Vendida + por vender " + dinero(t.gananciaPorVenderRebaja()), true),
                new Tarjeta("TOTAL CON CAMPAÑA", t.gananciaTotalCampana(), true, "Vendida + por vender " + dinero(t.gananciaPorVenderCampana()), true));
    }

    private static String dinero(BigDecimal b) {
        return "$" + new java.text.DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols.getInstance(java.util.Locale.US)).format(b);
    }
}
