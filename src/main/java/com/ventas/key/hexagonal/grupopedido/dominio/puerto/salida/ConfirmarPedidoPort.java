package com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida;

/**
 * Confirma un pedido de contado con el mismo camino que el boton "Cobrar" de la card.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 *
 * <p>Igual que con los abonos, no se reescribe el cobro: confirmar ya crea la venta, calcula la
 * ganancia con la comision de la terminal y deja el pedido Entregado.
 */
public interface ConfirmarPedidoPort {

    void confirmarDeContado(Integer pedidoId, Integer pagosYMesesId);
}
