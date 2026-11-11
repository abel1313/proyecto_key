package com.ventas.key.hexagonal.reportecategoria.dominio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Las cuentas del reporte. Dominio puro. */
public final class ReglasReporte {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal TOPE_PCT = new BigDecimal("999.99");

    private ReglasReporte() { }

    /** Margen sobre el precio de venta: (venta − costo) ÷ venta × 100, a 2 decimales. null si no hay costo o el precio es cero. */
    public static BigDecimal margenSobreVenta(BigDecimal costo, BigDecimal precioVenta) {
        if (costo == null || precioVenta == null || precioVenta.signum() <= 0) return null;
        BigDecimal m = precioVenta.subtract(costo).multiply(CIEN).divide(precioVenta, 2, RoundingMode.HALF_UP);
        return m.max(TOPE_PCT.negate()).min(TOPE_PCT);
    }

    /** Ganancia ÷ total vendido × 100. null si no se vendió nada. */
    public static BigDecimal margenReal(BigDecimal ganancia, BigDecimal total) {
        if (ganancia == null || total == null || total.signum() <= 0) return null;
        return ganancia.multiply(CIEN).divide(total, 2, RoundingMode.HALF_UP);
    }

    /** Lo que se ganaría vendiendo todo el stock al precio de venta: stock × (venta − costo). Cero si falta algún dato o el stock es cero. */
    public static BigDecimal gananciaPorVender(int stock, BigDecimal costo, BigDecimal precioVenta) {
        if (stock <= 0 || costo == null || precioVenta == null) return BigDecimal.ZERO.setScale(2);
        return precioVenta.subtract(costo).multiply(BigDecimal.valueOf(stock)).setScale(2, RoundingMode.HALF_UP);
    }

    /** Precio al que se vendería con rebaja: el de rebaja si tiene (mayor que 0); si no, el de venta. */
    public static BigDecimal precioConRebaja(FilaBase b) {
        return b.precioRebaja() != null && b.precioRebaja().signum() > 0 ? b.precioRebaja() : b.precioVenta();
    }

    /** Precio al que se vendería en campaña: el de campaña si el producto está en una; si no, el de venta. */
    public static BigDecimal precioEnCampana(FilaBase b) {
        return b.precioCampana() != null ? b.precioCampana() : b.precioVenta();
    }

    /** Flete que lleva lo ya vendido: flete por pieza (del último cálculo de precio) × piezas vendidas. */
    public static BigDecimal fleteDeLoVendido(FilaReporte f) {
        return f.flete().multiply(BigDecimal.valueOf(f.base().piezas())).setScale(2, RoundingMode.HALF_UP);
    }

    /** Flete que lleva lo que sigue en stock: flete por pieza × piezas en stock. */
    public static BigDecimal fleteEnStock(FilaReporte f) {
        return f.flete().multiply(BigDecimal.valueOf(Math.max(0, f.base().stock()))).setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean pasaVista(VistaReporte vista, FilaBase f) {
        return switch (vista) {
            case VENDIDOS -> f.piezas() > 0;
            case NO_VENDIDOS -> f.piezas() == 0;
            case TODOS -> true;
        };
    }

    public static TotalesReporte totales(List<FilaReporte> filas) {
        int vendidos = 0;
        long piezas = 0, porVender = 0;
        BigDecimal total = BigDecimal.ZERO, costo = BigDecimal.ZERO, ganV = BigDecimal.ZERO, ganP = BigDecimal.ZERO,
                ganPRebaja = BigDecimal.ZERO, ganPCampana = BigDecimal.ZERO, fleteV = BigDecimal.ZERO, fleteS = BigDecimal.ZERO,
                sumCompra = BigDecimal.ZERO, sumVenta = BigDecimal.ZERO, sumRebaja = BigDecimal.ZERO, sumCampana = BigDecimal.ZERO;
        for (FilaReporte f : filas) {
            FilaBase b = f.base();
            if (b.piezas() > 0) vendidos++;
            piezas += b.piezas();
            porVender += Math.max(0, b.stock());
            total = total.add(b.total());
            costo = costo.add(b.costo());
            ganV = ganV.add(b.ganancia());
            ganP = ganP.add(f.gananciaPorVender());
            ganPRebaja = ganPRebaja.add(f.gananciaPorVenderRebaja());
            if (b.precioCompra() != null) sumCompra = sumCompra.add(b.precioCompra());
            if (b.precioVenta() != null) sumVenta = sumVenta.add(b.precioVenta());
            if (b.precioRebaja() != null) sumRebaja = sumRebaja.add(b.precioRebaja());
            if (b.precioCampana() != null) sumCampana = sumCampana.add(b.precioCampana());
            fleteV = fleteV.add(fleteDeLoVendido(f));
            fleteS = fleteS.add(fleteEnStock(f));
            ganPCampana = ganPCampana.add(f.gananciaPorVenderCampana());
        }
        return new TotalesReporte(filas.size(), vendidos, filas.size() - vendidos, piezas, escala(total), escala(costo), escala(ganV),
                margenReal(ganV, total), porVender, escala(ganP), escala(ganV.add(ganP)),
                escala(ganPRebaja), escala(ganPCampana), escala(ganV.add(ganPRebaja)), escala(ganV.add(ganPCampana)), escala(fleteV), escala(fleteS), escala(sumCompra), escala(sumVenta), escala(sumRebaja), escala(sumCampana));
    }

    private static BigDecimal escala(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
