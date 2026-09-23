package com.ventas.key.hexagonal.grupopedido.dominio.excepcion;

/** No se puede separar asi: el reparto no cuadra, falta el titular o el pedido no esta en el grupo. */
public class SeparacionInvalidaException extends GrupoPedidoException {

    public SeparacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
