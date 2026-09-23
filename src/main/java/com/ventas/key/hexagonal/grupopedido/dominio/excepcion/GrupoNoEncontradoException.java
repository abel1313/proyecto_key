package com.ventas.key.hexagonal.grupopedido.dominio.excepcion;

public class GrupoNoEncontradoException extends GrupoPedidoException {

    public GrupoNoEncontradoException(Integer grupoId) {
        super("No existe el grupo de pedidos " + grupoId);
    }
}
