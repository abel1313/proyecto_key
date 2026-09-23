package com.ventas.key.hexagonal.grupopedido.dominio.modelo;

/**
 * La parte de un abono al grupo que le toca a un pedido.
 *
 * @param pedidoId     a que pedido va
 * @param montoCentavos cuanto se le abona
 * @param liquida      si con esta parte el pedido queda pagado
 */
public record Reparto(Integer pedidoId, long montoCentavos, boolean liquida) {
}
