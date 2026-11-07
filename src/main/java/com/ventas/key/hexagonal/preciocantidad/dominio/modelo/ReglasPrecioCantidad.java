package com.ventas.key.hexagonal.preciocantidad.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [Hexagonal: Domain] [Clean: Entity] Java puro. Reglas G3, G4 y G6 de {@code PLAN_CAMPANAS_Y_PRECIOS.md}.
 *
 * <ul>
 *   <li>G3: las piezas SOLO se suman entre productos del mismo grupo; cuando el total del grupo llega al mínimo, todos los productos del
 *       grupo que lleva la venta se cobran a su precio de grupo;</li>
 *   <li>G4: fuera de fechas o con el interruptor apagado, el grupo no cuenta;</li>
 *   <li>G6: se calcula de nuevo con lo que lleva la venta en ese momento (si baja del mínimo, desaparece).</li>
 * </ul>
 */
public final class ReglasPrecioCantidad {

    private ReglasPrecioCantidad() {
    }

    /**
     * El precio de grupo de cada producto de la venta al que le toca. Lo que no está en el resultado se cobra a su precio normal.
     * Si un producto apareciera en dos grupos (no debería: G2), se queda con el precio más bajo.
     */
    public static Map<Integer, PrecioDeGrupo> calcular(List<GrupoPrecioCantidad> grupos, List<LineaPorCantidad> lineas, LocalDate hoy) {
        Map<Integer, PrecioDeGrupo> resultado = new HashMap<>();
        if (grupos == null || lineas == null || lineas.isEmpty()) {
            return resultado;
        }
        Map<Integer, Integer> piezasPorProducto = new HashMap<>();
        for (LineaPorCantidad l : lineas) {
            if (l.cantidad() > 0) {
                piezasPorProducto.merge(l.productoId(), l.cantidad(), Integer::sum);
            }
        }
        for (GrupoPrecioCantidad g : grupos) {
            if (!g.vigenteEn(hoy)) {
                continue;
            }
            int piezas = g.miembros().stream().mapToInt(m -> piezasPorProducto.getOrDefault(m.productoId(), 0)).sum();
            if (piezas < g.minimoPiezas()) {
                continue;
            }
            for (MiembroDeGrupo m : g.miembros()) {
                if (!piezasPorProducto.containsKey(m.productoId())) {
                    continue;
                }
                PrecioDeGrupo nuevo = new PrecioDeGrupo(m.productoId(), g.id() == null ? 0 : g.id(), g.nombre(), m.precioPromocion(), g.minimoPiezas(), piezas);
                resultado.merge(m.productoId(), nuevo, (a, b) -> a.precio().compareTo(b.precio()) <= 0 ? a : b);
            }
        }
        return resultado;
    }

    /** G: gana el precio más bajo; los descuentos no se suman. */
    public static BigDecimal precioACobrar(BigDecimal precioBase, PrecioDeGrupo grupo) {
        if (grupo == null) {
            return precioBase;
        }
        return grupo.precio().compareTo(precioBase) < 0 ? grupo.precio() : precioBase;
    }
}
