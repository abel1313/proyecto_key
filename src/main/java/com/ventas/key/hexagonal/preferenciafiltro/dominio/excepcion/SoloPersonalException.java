package com.ventas.key.hexagonal.preferenciafiltro.dominio.excepcion;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>R1: un cliente no guarda filtros. El controller la devuelve como 403.
 */
public class SoloPersonalException extends RuntimeException {

    public SoloPersonalException() {
        super("Guardar filtros es solo para el personal de la tienda");
    }
}
