package com.ventas.key.hexagonal.cortecaja.dominio.modelo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * [Hexagonal: Domain] [Clean: Entity] Java puro.
 *
 * <p>El corte de caja: todo lo que falta por cobrar de los pedidos abiertos, por zona de entrega, con el total general.
 */
public record CorteDeCaja(Instant generado, List<CorteDeZona> zonas) {

    public CorteDeCaja {
        zonas = List.copyOf(zonas);
    }

    /** La suma de todas las zonas (incluido «Sin zona»). */
    public BigDecimal total() {
        return zonas.stream().map(CorteDeZona::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Costo y ganancia de todo el corte (solo administrador). */
    public MontosDeGanancia montos() {
        return zonas.stream().map(CorteDeZona::montos).reduce(MontosDeGanancia.CERO, MontosDeGanancia::mas);
    }

    public int cantidadPedidos() {
        return zonas.stream().mapToInt(CorteDeZona::cantidad).sum();
    }
}
