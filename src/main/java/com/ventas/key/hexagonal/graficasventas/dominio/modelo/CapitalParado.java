package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

import java.util.List;

/**
 * Mercancía con stock que no se ha vendido en {@code dias} días: cuántos artículos, cuántas piezas y cuánto dinero (a costo)
 * tienen guardado. {@code mayores} son los que más capital tienen parado.
 */
public record CapitalParado(int dias, int articulos, int piezas, double capital, List<CasoCapitalParado> mayores) {
    public CapitalParado {
        mayores = List.copyOf(mayores);
    }
}
