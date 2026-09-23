package com.ventas.key.hexagonal.grupopedido.dominio.excepcion;

/**
 * Una operacion sobre un grupo de pedidos que el dominio rechaza.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Raiz comun para que el controller mapee "el negocio dijo que no" a un 400 sin enumerar cada
 * caso, y sin confundirlo con un fallo tecnico (que sigue siendo 500).
 */
public class GrupoPedidoException extends RuntimeException {

    public GrupoPedidoException(String mensaje) {
        super(mensaje);
    }
}
