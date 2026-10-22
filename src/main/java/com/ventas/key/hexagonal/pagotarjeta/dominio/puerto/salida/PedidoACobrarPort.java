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
}
