package com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion;

/** Se pidio editar algo que no esta en el pedido, o que no existe en el catalogo. */
public class ArticuloNoEncontradoException extends EdicionPedidoException {

    public ArticuloNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public static ArticuloNoEncontradoException lineaDePedido(Integer pedidoId, Integer detalleId) {
        return new ArticuloNoEncontradoException(
                "La linea " + detalleId + " no existe en el pedido " + pedidoId);
    }

    public static ArticuloNoEncontradoException enCatalogo(Integer varianteId) {
        return new ArticuloNoEncontradoException("El articulo " + varianteId + " no existe");
    }
}
