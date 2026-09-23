package com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion;

/** El pedido ya se entrego o se cancelo: no se le editan articulos (R1). */
public class PedidoCerradoException extends EdicionPedidoException {

    public PedidoCerradoException(Integer pedidoId, String motivo) {
        super("El pedido " + pedidoId + " " + motivo + ": no se pueden editar sus articulos");
    }
}
