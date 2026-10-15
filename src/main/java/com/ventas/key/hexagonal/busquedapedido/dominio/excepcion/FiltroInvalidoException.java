package com.ventas.key.hexagonal.busquedapedido.dominio.excepcion;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Un filtro que no se puede aplicar (fechas al reves, total negativo, texto muy corto). Sale
 * como 400 con este mensaje.
 */
public class FiltroInvalidoException extends RuntimeException {

    public FiltroInvalidoException(String mensaje) {
        super(mensaje);
    }
}
