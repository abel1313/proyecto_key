package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

import java.util.List;

/**
 * Lo que te deben (Apartados e Ir pagando) por antigüedad. {@code total} es la misma cuenta que la tarjeta «Por cobrar»
 * del Dashboard. {@code masAntiguos} son los que más tiempo llevan, para saber a quién cobrarle primero.
 */
public record DineroPorCobrar(double total, int pedidos, List<TramoPorCobrar> tramos, List<CasoPorCobrar> masAntiguos) {
    public DineroPorCobrar {
        tramos = List.copyOf(tramos);
        masAntiguos = List.copyOf(masAntiguos);
    }
}
