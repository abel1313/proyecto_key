package com.ventas.key.hexagonal.flete.dominio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * [Hexagonal: Domain] [Clean: Entity] Java puro. Cómo va un viaje: cuánto flete ya se recuperó con lo vendido, cuánto falta, desde qué venta se
 * pagó y qué parte del flete de lo vendido después ya es ganancia. TODO se calcula al leer (con la historia completa del viaje, ordenada por
 * fecha de venta, luego id de venta y id de línea), no se guarda: así una devolución o una corrección no dejan números viejos.
 *
 * <p>Regla del dueño: el flete se paga cuando lo recuperado llega al «costo a cubrir» (flete + gastos que cuentan). Desde ahí, el flete de lo que
 * siga vendiéndose es ganancia. La línea que cruza el tope se parte: lo que falta para cubrir es recuperado; el resto es ganancia.
 */
public record ReporteDeViaje(ViajeDeCompra viaje, String estado, int piezasCompradas, int piezasVendidas, int piezasRestantes,
                             BigDecimal totalVendido, BigDecimal costoVendido, BigDecimal gananciaVendida,
                             BigDecimal fleteRecuperado, BigDecimal faltaPorCubrir, BigDecimal fleteComoGanancia,
                             Integer pagadoConVentaId, LocalDateTime pagadoEn, List<Renglon> renglones, List<Linea> lineas) {

    public static final String ABIERTO = "ABIERTO";
    public static final String FLETE_CUBIERTO = "FLETE_CUBIERTO";
    public static final String VENDIDO_POR_COMPLETO = "VENDIDO_POR_COMPLETO";

    /** Una venta ligada al viaje, en el orden en que se recuperó el flete, con la parte del flete que ya fue recuperada y la que ya es ganancia. */
    public record Linea(VentaLigada venta, BigDecimal fleteRecuperado, BigDecimal fleteComoGanancia) {
    }

    /** Cómo va cada producto del viaje. */
    public record Renglon(int productoId, String nombre, String codigoBarras, int piezasCompradas, int piezasVendidas, int piezasRestantes,
                          BigDecimal totalVendido) {
    }

    public static ReporteDeViaje calcular(ViajeDeCompra viaje, List<VentaLigada> ligadas) {
        List<VentaLigada> orden = ligadas.stream()
                .sorted(Comparator.comparing(VentaLigada::fechaVenta, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparingInt(VentaLigada::ventaId).thenComparingInt(VentaLigada::detalleVentaId))
                .toList();
        BigDecimal tope = viaje.costoACubrir();
        BigDecimal recuperado = BigDecimal.ZERO;
        BigDecimal ganancia = BigDecimal.ZERO;
        BigDecimal totalVendido = BigDecimal.ZERO;
        BigDecimal costo = BigDecimal.ZERO;
        BigDecimal gananciaVentas = BigDecimal.ZERO;
        int vendidas = 0;
        Integer pagadoCon = null;
        LocalDateTime pagadoEn = null;
        List<Linea> lineas = new java.util.ArrayList<>();
        Map<Integer, Integer> piezasPorProducto = new TreeMap<>();
        Map<Integer, BigDecimal> totalPorProducto = new TreeMap<>();
        for (VentaLigada l : orden) {
            BigDecimal fleteLinea = l.fletePorPieza().multiply(BigDecimal.valueOf(l.cantidad()));
            BigDecimal falta = tope.subtract(recuperado).max(BigDecimal.ZERO);
            BigDecimal aRecuperar = fleteLinea.min(falta);
            recuperado = recuperado.add(aRecuperar);
            ganancia = ganancia.add(fleteLinea.subtract(aRecuperar));
            lineas.add(new Linea(l, esc(aRecuperar), esc(fleteLinea.subtract(aRecuperar))));
            if (pagadoCon == null && tope.signum() > 0 && recuperado.compareTo(tope) >= 0) {
                pagadoCon = l.ventaId();
                pagadoEn = l.fechaVenta();
            }
            vendidas += l.cantidad();
            totalVendido = totalVendido.add(l.totalVendido());
            costo = costo.add(l.costo());
            gananciaVentas = gananciaVentas.add(l.ganancia());
            piezasPorProducto.merge(l.productoId(), l.cantidad(), Integer::sum);
            totalPorProducto.merge(l.productoId(), l.totalVendido(), BigDecimal::add);
        }
        int compradas = viaje.piezasTotal();
        int restantes = Math.max(0, compradas - vendidas);
        BigDecimal falta = tope.subtract(recuperado).max(BigDecimal.ZERO);
        String estado = compradas > 0 && restantes == 0 ? VENDIDO_POR_COMPLETO
                : (tope.signum() > 0 ? (falta.signum() == 0 ? FLETE_CUBIERTO : ABIERTO) : (vendidas > 0 ? FLETE_CUBIERTO : ABIERTO));
        List<Renglon> renglones = viaje.productos().stream().map(p -> {
            int v = piezasPorProducto.getOrDefault(p.productoId(), 0);
            return new Renglon(p.productoId(), p.nombre(), p.codigoBarras(), p.piezas(), v, Math.max(0, p.piezas() - v),
                    totalPorProducto.getOrDefault(p.productoId(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        }).toList();
        return new ReporteDeViaje(viaje, estado, compradas, vendidas, restantes, esc(totalVendido), esc(costo), esc(gananciaVentas),
                esc(recuperado), esc(falta), esc(ganancia), pagadoCon, pagadoEn, renglones, List.copyOf(lineas));
    }

    private static BigDecimal esc(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
