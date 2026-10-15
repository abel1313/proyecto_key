package com.ventas.key.hexagonal.datoslegales.dominio.excepcion;

/** Un dato del negocio no tiene el formato que pide la ley o el sistema. Mensaje para el dueño. */
public class DatosLegalesInvalidosException extends RuntimeException {
    public DatosLegalesInvalidosException(String mensaje) {
        super(mensaje);
    }
}
