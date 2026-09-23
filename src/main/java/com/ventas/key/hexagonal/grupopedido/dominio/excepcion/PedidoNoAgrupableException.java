package com.ventas.key.hexagonal.grupopedido.dominio.excepcion;

/** Un pedido no puede entrar al grupo: no existe, esta cerrado o ya esta en otro grupo. */
public class PedidoNoAgrupableException extends GrupoPedidoException {

    public PedidoNoAgrupableException(String mensaje) {
        super(mensaje);
    }
}
