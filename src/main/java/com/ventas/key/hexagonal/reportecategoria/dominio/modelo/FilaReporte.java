package com.ventas.key.hexagonal.reportecategoria.dominio.modelo;

import java.math.BigDecimal;

/**
 * Una fila del reporte: el producto con lo vendido y los porcentajes.
 * {@code margenCalculadoPct}: el del último cálculo de precio guardado (o el calculado sobre la marcha, sin flete, si no tenía).
 * {@code margenRealPct}: ganancia ÷ total vendido (null si no se vendió). {@code gananciaPorVender*}: lo que quedaría de ganancia si se vende
 * todo el stock al precio de venta, al de rebaja o al de campaña (los que no tienen rebaja o campaña usan el de venta). {@code sinFlete}: el cálculo no llevó flete (transporte en cero) o no hay cálculo.
 */
public record FilaReporte(FilaBase base, BigDecimal margenCalculadoPct, boolean calculoNuevo, BigDecimal flete, boolean sinFlete,
                          BigDecimal margenRealPct, BigDecimal gananciaPorVender, BigDecimal gananciaPorVenderRebaja,
                          BigDecimal gananciaPorVenderCampana) {
}
