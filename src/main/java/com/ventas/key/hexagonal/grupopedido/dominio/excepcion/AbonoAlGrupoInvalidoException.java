package com.ventas.key.hexagonal.grupopedido.dominio.excepcion;

/** El abono al grupo no se puede registrar: monto fuera de rango, grupo de contado o deshecho. */
public class AbonoAlGrupoInvalidoException extends GrupoPedidoException {

    public AbonoAlGrupoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
