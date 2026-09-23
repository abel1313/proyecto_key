package com.ventas.key.hexagonal.grupopedido.dominio.modelo;

/**
 * Pasar {@code centavos} del abono {@code abonoId} del pedido {@code dePedidoId} al {@code aPedidoId}.
 * Si es menos que todo el abono, el abono se parte en dos con la misma fecha y forma de pago.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 */
public record Movimiento(Integer abonoId, Integer dePedidoId, Integer aPedidoId, long centavos) {
}
