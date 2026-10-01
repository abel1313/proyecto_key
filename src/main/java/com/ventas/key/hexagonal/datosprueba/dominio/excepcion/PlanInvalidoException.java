package com.ventas.key.hexagonal.datosprueba.dominio.excepcion;

/** R11: los topes de una corrida. */
public class PlanInvalidoException extends DatosPruebaException {
    public PlanInvalidoException(String mensaje) {
        super(mensaje);
    }
}
