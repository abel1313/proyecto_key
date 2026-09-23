package com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion;

/**
 * Una edicion que el dominio rechaza.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Raiz comun para que el controller pueda mapear "el negocio dijo que no" a un 400 sin
 * enumerar cada caso, y sin confundirlo con un fallo tecnico (que debe seguir siendo 500).
 */
public class EdicionPedidoException extends RuntimeException {

    public EdicionPedidoException(String mensaje) {
        super(mensaje);
    }
}
