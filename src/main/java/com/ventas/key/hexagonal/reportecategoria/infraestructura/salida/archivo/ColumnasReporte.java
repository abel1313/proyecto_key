package com.ventas.key.hexagonal.reportecategoria.infraestructura.salida.archivo;

import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.FilaReporte;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.FiltroReporte;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.TotalesReporte;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/** Las columnas del reporte, en un solo lugar para que el Excel y el PDF salgan iguales. */
final class ColumnasReporte {

    enum Tipo { TEXTO, DINERO, PORCENTAJE, ENTERO }

    record Columna(String titulo, Tipo tipo, Function<FilaReporte, Object> valor, Function<TotalesReporte, Object> total) { }

    /** Piezas de un tipo de precio: vacío (no cero) si ese producto no se vendió así. */
    private static Object pzs(long piezas) { return piezas > 0 ? (Object) piezas : null; }
    private static Object din(long piezas, BigDecimal monto) { return piezas > 0 ? monto : null; }
    private static Object precio(long piezas, BigDecimal monto) { return com.ventas.key.hexagonal.reportecategoria.dominio.modelo.ReglasReporte.precioPromedio(piezas, monto); }

    /**
     * Un solo reporte: primero lo que se invirtió (compra y flete), luego lo vendido y a qué precio se vendió (normal, rebaja, campaña, otro:
     * piezas, precio y dinero; vacío si ese producto no se vendió así) y al final lo que falta por vender.
     */
    static final List<Columna> COLUMNAS = List.of(
            new Columna("Producto", Tipo.TEXTO, f -> f.base().nombre(), t -> "TOTALES (" + t.productos() + " productos)"),
            new Columna("Código de barras", Tipo.TEXTO, f -> f.base().codigoBarras(), t -> null),
            new Columna("Categoría", Tipo.TEXTO, f -> f.base().categoria() == null ? "Otros" : f.base().categoria(), t -> null),
            new Columna("Precio compra", Tipo.DINERO, f -> f.base().precioCompra(), TotalesReporte::sumaCompra),
            new Columna("Precio venta", Tipo.DINERO, f -> f.base().precioVenta(), TotalesReporte::sumaVenta),
            new Columna("Precio rebaja", Tipo.DINERO, f -> f.base().precioRebaja(), TotalesReporte::sumaRebaja),
            new Columna("Rebaja campaña", Tipo.DINERO, f -> f.base().precioCampana(), TotalesReporte::sumaCampana),
            new Columna("Inversión", Tipo.DINERO, com.ventas.key.hexagonal.reportecategoria.dominio.modelo.ReglasReporte::inversion, TotalesReporte::inversion),
            new Columna("Flete", Tipo.DINERO, com.ventas.key.hexagonal.reportecategoria.dominio.modelo.ReglasReporte::fleteTotal, t -> t.fleteVendido().add(t.fleteEnStock())),
            new Columna("¿Flete?", Tipo.TEXTO, f -> f.sinFlete() ? "SIN FLETE" : "Con flete", t -> null),
            new Columna("Piezas vendidas", Tipo.ENTERO, f -> f.base().piezas(), TotalesReporte::piezas),
            new Columna("Total vendido", Tipo.DINERO, f -> f.base().total(), TotalesReporte::total),
            new Columna("Ganancia vendida", Tipo.DINERO, f -> f.base().ganancia(), TotalesReporte::gananciaVendida),
            new Columna("% real", Tipo.PORCENTAJE, FilaReporte::margenRealPct, TotalesReporte::margenRealPct),
            new Columna("Normal: piezas", Tipo.ENTERO, f -> pzs(f.base().piezasVenta()), TotalesReporte::piezasVenta),
            new Columna("Normal: precio", Tipo.DINERO, f -> precio(f.base().piezasVenta(), f.base().montoVenta()), t -> null),
            new Columna("Normal: total", Tipo.DINERO, f -> din(f.base().piezasVenta(), f.base().montoVenta()), TotalesReporte::montoVenta),
            new Columna("Rebaja: piezas", Tipo.ENTERO, f -> pzs(f.base().piezasRebaja()), TotalesReporte::piezasRebaja),
            new Columna("Rebaja: precio", Tipo.DINERO, f -> precio(f.base().piezasRebaja(), f.base().montoRebaja()), t -> null),
            new Columna("Rebaja: total", Tipo.DINERO, f -> din(f.base().piezasRebaja(), f.base().montoRebaja()), TotalesReporte::montoRebaja),
            new Columna("Campaña: piezas", Tipo.ENTERO, f -> pzs(f.base().piezasCampana()), TotalesReporte::piezasCampana),
            new Columna("Campaña: precio", Tipo.DINERO, f -> precio(f.base().piezasCampana(), f.base().montoCampana()), t -> null),
            new Columna("Campaña: total", Tipo.DINERO, f -> din(f.base().piezasCampana(), f.base().montoCampana()), TotalesReporte::montoCampana),
            new Columna("Otro: piezas", Tipo.ENTERO, f -> pzs(f.base().piezasOtro()), TotalesReporte::piezasOtro),
            new Columna("Otro: total", Tipo.DINERO, f -> din(f.base().piezasOtro(), f.base().montoOtro()), TotalesReporte::montoOtro),
            new Columna("Piezas con tipo estimado", Tipo.ENTERO, f -> pzs(f.base().piezasEstimadas()), TotalesReporte::piezasEstimadas),
            new Columna("Por vender (stock)", Tipo.ENTERO, f -> (long) Math.max(0, f.base().stock()), TotalesReporte::piezasPorVender),
            new Columna("Por vender: venta", Tipo.DINERO, FilaReporte::gananciaPorVender, TotalesReporte::gananciaPorVender),
            new Columna("Por vender: rebaja", Tipo.DINERO, FilaReporte::gananciaPorVenderRebaja, TotalesReporte::gananciaPorVenderRebaja),
            new Columna("Por vender: campaña", Tipo.DINERO, FilaReporte::gananciaPorVenderCampana, TotalesReporte::gananciaPorVenderCampana),
            new Columna("Total: venta", Tipo.DINERO, f -> f.base().ganancia().add(f.gananciaPorVender()), TotalesReporte::gananciaTotal),
            new Columna("Total: rebaja", Tipo.DINERO, f -> f.base().ganancia().add(f.gananciaPorVenderRebaja()), TotalesReporte::gananciaTotalRebaja),
            new Columna("Total: campaña", Tipo.DINERO, f -> f.base().ganancia().add(f.gananciaPorVenderCampana()), TotalesReporte::gananciaTotalCampana)
    );

    /** Cómo leer las columnas: va en el PDF, el Excel y la pantalla para que no haya dudas. */
    static final String LEYENDA = "Inversión = precio de compra × (piezas vendidas + piezas en stock). Flete = flete por pieza del último cálculo de precio × esas mismas piezas; «SIN FLETE» = el producto no lo trae capturado. "
            + "Total vendido y ganancia vendida: lo realmente cobrado en el periodo (la ganancia ya descuenta la comisión de tarjeta). "
            + "Normal / rebaja / campaña / otro: a qué precio se vendió (piezas, precio promedio y dinero); queda vacío si ese producto no se vendió así; «otro» = precio por cantidad, promoción u otro monto. "
            + "Las ventas hechas antes de que se guardara el tipo de precio se clasifican comparando lo cobrado con los precios actuales y se cuentan en «Piezas con tipo estimado» (pueden equivocarse si el precio cambió). "
            + "Por vender = piezas en stock × (precio - compra) con el precio de venta, de rebaja o de campaña; «Total» = ganancia vendida + por vender; no descuentan flete ni comisión.";

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ColumnasReporte() { }

    static String descripcion(FiltroReporte f) {
        String que = f.porProductos() ? f.productoIds().size() + " productos elegidos"
                : f.categoria() != null ? "Categoría " + f.categoria()
                : f.sinCategoria() ? "Otros (sin categoría)" : "Todas las categorías";
        return que + " · del " + DIA.format(f.desde()) + " al " + DIA.format(f.hasta()) + " · " + switch (f.vista()) {
            case VENDIDOS -> "solo lo vendido";
            case NO_VENDIDOS -> "solo lo que no se ha vendido";
            case TODOS -> "vendido y por vender";
        };
    }

    static boolean esNumero(Object v) {
        return v instanceof BigDecimal || v instanceof Number;
    }
}
