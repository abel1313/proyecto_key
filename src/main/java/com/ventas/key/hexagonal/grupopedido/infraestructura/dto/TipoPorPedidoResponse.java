package com.ventas.key.hexagonal.grupopedido.infraestructura.dto;

/** Una fila del 400 de "distinta forma de cobro": que tipo tiene cada pedido. */
public record TipoPorPedidoResponse(Integer pedidoId, String tipoPedido) {
}
