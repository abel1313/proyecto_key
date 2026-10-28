package com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.PedidoACobrar;

import java.util.Optional;

/**
 * Lee el pedido tal como el cobro lo necesita. Solo lee: marcarlo pagado es de {@link ActualizarPedidoPort}.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 */
public interface PedidoACobrarPort {

    Optional<PedidoACobrar> buscar(Integer pedidoId);

    /**
     * ¿Hay un cobro de la terminal en proceso por el camino ANTERIOR ({@code /v1/mp})? Mientras los dos caminos
     * convivan, un pedido no se cobra por los dos a la vez (doble cobro). Por defecto no (ambientes sin ese camino).
     */
    default boolean hayCobroEnProcesoPorElCaminoAnterior(Integer pedidoId) {
        return false;
    }
}
