package com.ventas.key.hexagonal.campana.dominio.excepcion;

/** Una regla de la campaña no se cumple. El mensaje va en palabras del dueño. */
public class CampanaInvalidaException extends RuntimeException {
    public CampanaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
