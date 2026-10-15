package com.ventas.key.hexagonal.datosprueba.dominio.excepcion;

/** Base de los errores del generador de datos de prueba. El mensaje es para el usuario. */
public class DatosPruebaException extends RuntimeException {
    public DatosPruebaException(String mensaje) {
        super(mensaje);
    }
}
