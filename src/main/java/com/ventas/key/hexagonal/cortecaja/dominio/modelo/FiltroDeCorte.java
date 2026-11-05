package com.ventas.key.hexagonal.cortecaja.dominio.modelo;

import com.ventas.key.hexagonal.cortecaja.dominio.excepcion.CorteDeCajaException;

import java.time.LocalDate;
import java.util.Set;

/**
 * [Hexagonal: Domain] [Clean: Entity] Java puro.
 *
 * <p>Qué pedidos entran al corte. Siempre quedan fuera los cancelados.
 *
 * @param zonas  las zonas de entrega que entran (varias a la vez); vacío = todas. El id {@link #SIN_ZONA} (0) agrega «Sin zona»
 *               (pedidos que se recogen en tienda)
 * @param desde  primer día de la FECHA DE ENTREGA (la que se programa en Entregas por zona); un pedido sin fecha de entrega no entra cuando hay fechas; {@code null} = sin límite
 * @param hasta  último día de la fecha de entrega; {@code null} = sin límite
 * @param pago   cuáles según su pago
 * @param entrega cuáles según su entrega
 */
public record FiltroDeCorte(Set<Integer> zonas, LocalDate desde, LocalDate hasta, Pago pago, Entrega entrega) {

    /** Id especial para pedir los pedidos sin zona de entrega. */
    public static final int SIN_ZONA = 0;

    /** Por su pago: lo que todavía se debe, lo ya pagado completo, o todo. */
    public enum Pago { POR_COBRAR, PAGADOS, TODOS }

    /** Por su entrega: lo que aún no se lleva, lo ya entregado, o todo. */
    public enum Entrega { SIN_ENTREGAR, ENTREGADOS, TODOS }

    public FiltroDeCorte {
        zonas = zonas == null ? Set.of() : Set.copyOf(zonas);
        pago = pago == null ? Pago.POR_COBRAR : pago;
        entrega = entrega == null ? Entrega.SIN_ENTREGAR : entrega;
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new CorteDeCajaException.FechasInvalidas("La fecha «desde» no puede ser después de «hasta».");
        }
    }

    public boolean todasLasZonas() {
        return zonas.isEmpty();
    }

    public boolean incluyeSinZona() {
        return zonas.contains(SIN_ZONA);
    }

    /** Los ids de zonas reales elegidas (sin el 0 de «Sin zona»). */
    public Set<Integer> zonasReales() {
        return zonas.stream().filter(z -> z != SIN_ZONA).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /** Lo que se pedía en el corte original: por cobrar y sin entregar, sin fechas, todas las zonas. */
    public static FiltroDeCorte porDefecto() {
        return new FiltroDeCorte(Set.of(), null, null, Pago.POR_COBRAR, Entrega.SIN_ENTREGAR);
    }
}
