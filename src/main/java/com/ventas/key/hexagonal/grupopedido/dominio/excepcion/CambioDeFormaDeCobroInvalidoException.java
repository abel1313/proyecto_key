package com.ventas.key.hexagonal.grupopedido.dominio.excepcion;

/** La forma de cobro del grupo no se puede cambiar a la que se pidió (R20). */
public class CambioDeFormaDeCobroInvalidoException extends GrupoPedidoException {

    public CambioDeFormaDeCobroInvalidoException(String mensaje) {
        super(mensaje);
    }
}
