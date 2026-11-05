package com.ventas.key.hexagonal.cortecaja.dominio.modelo;

import java.math.BigDecimal;
import java.util.List;

/**
 * [Hexagonal: Domain] [Clean: Entity] Java puro.
 *
 * <p>Los pedidos por cobrar de una zona. {@code zonaId} y {@code zona} son {@code null} en el grupo «Sin zona» (pedidos que se recogen en
 * la tienda).
 */
public record CorteDeZona(Integer zonaId, String zona, List<PedidoPorCobrar> pedidos) {

    public CorteDeZona {
        pedidos = List.copyOf(pedidos);
    }

    /** La suma de lo que falta por cobrar en esta zona. */
    public BigDecimal subtotal() {
        return pedidos.stream().map(PedidoPorCobrar::saldo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int cantidad() {
        return pedidos.size();
    }

    /** La suma de las piezas de los pedidos de la zona. */
    public int articulos() {
        return pedidos.stream().mapToInt(PedidoPorCobrar::articulos).sum();
    }

    /** Costo y ganancia de la zona (solo administrador). */
    public MontosDeGanancia montos() {
        return pedidos.stream().map(PedidoPorCobrar::montos).reduce(MontosDeGanancia.CERO, MontosDeGanancia::mas);
    }
}
