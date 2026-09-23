package com.ventas.key.hexagonal.grupopedido.dominio.excepcion;

/** El grupo no se puede cobrar de contado: es a credito, se deshizo o ya no queda nada por cobrar. */
public class CobroDelGrupoInvalidoException extends GrupoPedidoException {

    public CobroDelGrupoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
