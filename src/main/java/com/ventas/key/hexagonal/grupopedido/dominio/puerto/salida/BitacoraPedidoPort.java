package com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida;

/**
 * Deja escrito en las observaciones del pedido lo que le paso (R10).
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 */
public interface BitacoraPedidoPort {

    void anotar(Integer pedidoId, String texto);
}
