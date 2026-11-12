package com.ventas.key.hexagonal.flete.dominio.excepcion;

/** [Hexagonal: Domain] Una regla del viaje de compra no se cumple; el mensaje está en palabras del dueño. */
public class ViajeInvalidoException extends RuntimeException {
    public ViajeInvalidoException(String mensaje) {
        super(mensaje);
    }
}
