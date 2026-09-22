package com.ventas.key.hexagonal.rifa.dominio.excepcion;

/**
 * Base de los errores de carga de boletos. El controller la traduce a 400.
 *
 * <p>[Hexagonal: Domain Exception] [Clean: Entity]</p>
 */
public class CargaBoletosException extends RuntimeException {

    public CargaBoletosException(String mensaje) {
        super(mensaje);
    }
}
