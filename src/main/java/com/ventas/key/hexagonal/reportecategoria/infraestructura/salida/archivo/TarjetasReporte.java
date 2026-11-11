package com.ventas.key.hexagonal.reportecategoria.infraestructura.salida.archivo;

import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.TotalesReporte;

import java.math.BigDecimal;
import java.util.List;

/** Las 8 tarjetas con los totales de arriba del Excel y del PDF. */
final class TarjetasReporte {

    record Tarjeta(String etiqueta, BigDecimal valor, boolean dinero, String sub, boolean verde) { }

    private TarjetasReporte() { }

    static List<Tarjeta> de(TotalesReporte t) {
        String margen = t.margenRealPct() == null ? "" : String.format(java.util.Locale.US, "%.2f%% real", t.margenRealPct());
        return List.of(
                new Tarjeta("PRODUCTOS", BigDecimal.valueOf(t.productos()), false, t.productosVendidos() + " vendidos · " + t.productosSinVender() + " sin vender", false),
                new Tarjeta("INVERSIÓN", t.inversion(), true, "Compra × (vendidas + en stock)", false),
                new Tarjeta("FLETE", t.fleteVendido().add(t.fleteEnStock()), true, "Vendido " + dinero(t.fleteVendido()) + " · stock " + dinero(t.fleteEnStock()), false),
                new Tarjeta("TOTAL VENDIDO", t.total(), true, t.piezas() + " piezas · costo " + dinero(t.costo()), false),
                new Tarjeta("GANANCIA VENDIDA", t.gananciaVendida(), true, margen, true),
                new Tarjeta("A PRECIO NORMAL", t.montoVenta(), true, t.piezasVenta() + " piezas", false),
                new Tarjeta("A REBAJA", t.montoRebaja(), true, t.piezasRebaja() + " piezas", false),
                new Tarjeta("A CAMPAÑA", t.montoCampana(), true, t.piezasCampana() + " piezas · otro " + dinero(t.montoOtro()), false));
    }

    private static String dinero(BigDecimal b) {
        return "$" + new java.text.DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols.getInstance(java.util.Locale.US)).format(b);
    }
}
