package com.ventas.key.hexagonal.reportecategoria.infraestructura.salida.archivo;

import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.FilaReporte;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.FiltroReporte;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.TipoReporte;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.TotalesReporte;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/** Las columnas del reporte, en un solo lugar para que el Excel y el PDF salgan iguales. */
final class ColumnasReporte {

    enum Tipo { TEXTO, DINERO, PORCENTAJE, ENTERO }

    record Columna(String titulo, Tipo tipo, Function<FilaReporte, Object> valor, Function<TotalesReporte, Object> total) { }

    static final List<Columna> COLUMNAS = List.of(
            new Columna("Producto", Tipo.TEXTO, f -> f.base().nombre(), t -> "TOTALES (" + t.productos() + " productos)"),
            new Columna("Código de barras", Tipo.TEXTO, f -> f.base().codigoBarras(), t -> null),
            new Columna("Categoría", Tipo.TEXTO, f -> f.base().categoria() == null ? "Otros" : f.base().categoria(), t -> null),
            new Columna("Precio compra", Tipo.DINERO, f -> f.base().precioCompra(), TotalesReporte::sumaCompra),
            new Columna("Precio venta", Tipo.DINERO, f -> f.base().precioVenta(), TotalesReporte::sumaVenta),
            new Columna("Precio rebaja", Tipo.DINERO, f -> f.base().precioRebaja(), TotalesReporte::sumaRebaja),
            new Columna("Rebaja campaña", Tipo.DINERO, f -> f.base().precioCampana(), TotalesReporte::sumaCampana),
            new Columna("Piezas vendidas", Tipo.ENTERO, f -> f.base().piezas(), TotalesReporte::piezas),
            new Columna("Ventas", Tipo.ENTERO, f -> f.base().ventas(), t -> null),
            new Columna("Total vendido", Tipo.DINERO, f -> f.base().total(), TotalesReporte::total),
            new Columna("Costo vendido", Tipo.DINERO, f -> f.base().costo(), TotalesReporte::costo),
            new Columna("Ganancia vendida", Tipo.DINERO, f -> f.base().ganancia(), TotalesReporte::gananciaVendida),
            new Columna("% calculado", Tipo.PORCENTAJE, FilaReporte::margenCalculadoPct, t -> null),
            new Columna("% real", Tipo.PORCENTAJE, FilaReporte::margenRealPct, TotalesReporte::margenRealPct),
            new Columna("Flete por pieza", Tipo.DINERO, FilaReporte::flete, t -> null),
            new Columna("¿Flete?", Tipo.TEXTO, f -> f.sinFlete() ? "SIN FLETE" : "Con flete", t -> null),
            new Columna("Flete en lo vendido", Tipo.DINERO, com.ventas.key.hexagonal.reportecategoria.dominio.modelo.ReglasReporte::fleteDeLoVendido, TotalesReporte::fleteVendido),
            new Columna("Flete en stock", Tipo.DINERO, com.ventas.key.hexagonal.reportecategoria.dominio.modelo.ReglasReporte::fleteEnStock, TotalesReporte::fleteEnStock),
            new Columna("Por vender (stock)", Tipo.ENTERO, f -> (long) Math.max(0, f.base().stock()), TotalesReporte::piezasPorVender),
            new Columna("Por vender: venta", Tipo.DINERO, FilaReporte::gananciaPorVender, TotalesReporte::gananciaPorVender),
            new Columna("Por vender: rebaja", Tipo.DINERO, FilaReporte::gananciaPorVenderRebaja, TotalesReporte::gananciaPorVenderRebaja),
            new Columna("Por vender: campaña", Tipo.DINERO, FilaReporte::gananciaPorVenderCampana, TotalesReporte::gananciaPorVenderCampana),
            new Columna("Total: venta", Tipo.DINERO, f -> f.base().ganancia().add(f.gananciaPorVender()), TotalesReporte::gananciaTotal),
            new Columna("Total: rebaja", Tipo.DINERO, f -> f.base().ganancia().add(f.gananciaPorVenderRebaja()), TotalesReporte::gananciaTotalRebaja),
            new Columna("Total: campaña", Tipo.DINERO, f -> f.base().ganancia().add(f.gananciaPorVenderCampana()), TotalesReporte::gananciaTotalCampana)
    );

    /** Cómo leer las columnas: va en el PDF, el Excel y la pantalla para que no haya dudas. */
    static final String LEYENDA = "Total vendido y ganancia vendida: lo realmente cobrado en las ventas del periodo (a su precio de ese día; la ganancia ya descuenta la comisión de tarjeta). "
            + "Por vender = piezas en stock × (precio - compra) si se venden al precio de venta, al de rebaja o al de campaña (sin rebaja o campaña usa el de venta); no descuenta flete ni comisión. "
            + "Total = ganancia vendida + por vender, con cada precio. "
            + "Flete: el flete por pieza del último cálculo de precio de cada producto × las piezas vendidas (o en stock); «SIN FLETE» = ese producto no lo trae capturado.";

    /** Columnas del reporte «Ventas»: lo vendido repartido por tipo de precio (venta, rebaja, campaña, otro). */
    static final List<Columna> COLUMNAS_VENTAS = List.of(
            new Columna("Producto", Tipo.TEXTO, f -> f.base().nombre(), t -> "TOTALES (" + t.productos() + " productos)"),
            new Columna("Código de barras", Tipo.TEXTO, f -> f.base().codigoBarras(), t -> null),
            new Columna("Categoría", Tipo.TEXTO, f -> f.base().categoria() == null ? "Otros" : f.base().categoria(), t -> null),
            new Columna("Piezas vendidas", Tipo.ENTERO, f -> f.base().piezas(), TotalesReporte::piezas),
            new Columna("Ventas", Tipo.ENTERO, f -> f.base().ventas(), t -> null),
            new Columna("Total vendido", Tipo.DINERO, f -> f.base().total(), TotalesReporte::total),
            new Columna("Ganancia vendida", Tipo.DINERO, f -> f.base().ganancia(), TotalesReporte::gananciaVendida),
            new Columna("% real", Tipo.PORCENTAJE, FilaReporte::margenRealPct, TotalesReporte::margenRealPct),
            new Columna("Piezas a precio de venta", Tipo.ENTERO, f -> f.base().piezasVenta(), TotalesReporte::piezasVenta),
            new Columna("$ a precio de venta", Tipo.DINERO, f -> f.base().montoVenta(), TotalesReporte::montoVenta),
            new Columna("Piezas a rebaja", Tipo.ENTERO, f -> f.base().piezasRebaja(), TotalesReporte::piezasRebaja),
            new Columna("$ a rebaja", Tipo.DINERO, f -> f.base().montoRebaja(), TotalesReporte::montoRebaja),
            new Columna("Piezas a campaña", Tipo.ENTERO, f -> f.base().piezasCampana(), TotalesReporte::piezasCampana),
            new Columna("$ a campaña", Tipo.DINERO, f -> f.base().montoCampana(), TotalesReporte::montoCampana),
            new Columna("Piezas a otro precio", Tipo.ENTERO, f -> f.base().piezasOtro(), TotalesReporte::piezasOtro),
            new Columna("$ a otro precio", Tipo.DINERO, f -> f.base().montoOtro(), TotalesReporte::montoOtro),
            new Columna("Piezas con tipo estimado", Tipo.ENTERO, f -> f.base().piezasEstimadas(), TotalesReporte::piezasEstimadas)
    );

    static final String LEYENDA_VENTAS = "A qué precio se vendió lo vendido en el periodo. «Venta» = el precio normal; «rebaja» = el precio con descuento del producto o artículo; "
            + "«campaña» = el precio de campaña; «otro» = precio de grupo por cantidad, promoción o cualquier otro precio. Las ventas hechas desde que se activó el tipo de precio "
            + "lo traen guardado (exacto). Las anteriores NO lo guardaban: se clasifican comparando el precio cobrado con los precios actuales del producto y se cuentan en «Piezas con tipo estimado»; "
            + "si el precio del producto cambió desde entonces, esa estimación puede equivocarse. Total vendido y ganancia vendida son lo realmente cobrado (la ganancia ya descuenta la comisión de tarjeta).";

    static List<Columna> columnas(TipoReporte tipo) {
        return tipo == TipoReporte.VENTAS ? COLUMNAS_VENTAS : COLUMNAS;
    }

    static String leyenda(TipoReporte tipo) {
        return tipo == TipoReporte.VENTAS ? LEYENDA_VENTAS : LEYENDA;
    }

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ColumnasReporte() { }

    static String descripcion(FiltroReporte f) {
        String que = f.porProductos() ? f.productoIds().size() + " productos elegidos"
                : f.categoria() != null ? "Categoría " + f.categoria()
                : f.sinCategoria() ? "Otros (sin categoría)" : "Todas las categorías";
        return (f.tipo() == TipoReporte.VENTAS ? "Ventas por tipo de precio · " : "") + que + " · del " + DIA.format(f.desde()) + " al " + DIA.format(f.hasta()) + " · " + switch (f.vista()) {
            case VENDIDOS -> "solo lo vendido";
            case NO_VENDIDOS -> "solo lo que no se ha vendido";
            case TODOS -> "vendido y por vender";
        };
    }

    static boolean esNumero(Object v) {
        return v instanceof BigDecimal || v instanceof Number;
    }
}
