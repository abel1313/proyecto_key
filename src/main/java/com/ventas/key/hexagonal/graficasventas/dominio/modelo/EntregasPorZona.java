package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

import java.time.LocalDate;
import java.util.List;

/** A dónde van los pedidos del periodo, del lugar con más pedidos al que menos. Pedidos sin lugar salen como «Sin lugar de entrega». */
public record EntregasPorZona(LocalDate desde, LocalDate hasta, int pedidos, double total, List<ParteDeEntregas> zonas) {
    public EntregasPorZona {
        zonas = List.copyOf(zonas);
    }
}
