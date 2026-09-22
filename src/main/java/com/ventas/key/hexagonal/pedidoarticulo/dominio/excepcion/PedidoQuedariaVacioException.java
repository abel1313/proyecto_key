package com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion;

/**
 * La edicion dejaria el pedido sin ningun articulo.
 *
 * <p>Un pedido vacio no es un pedido. Si el cliente ya no quiere nada se cancela, que ademas
 * registra el motivo y devuelve el stock por el camino que corresponde.
 */
public class PedidoQuedariaVacioException extends EdicionPedidoException {

    public PedidoQuedariaVacioException(Integer pedidoId) {
        super("El pedido " + pedidoId + " quedaria sin articulos. Si ya no se quiere nada, "
                + "hay que cancelar el pedido en vez de vaciarlo");
    }
}
