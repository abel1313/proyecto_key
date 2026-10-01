package com.ventas.key.hexagonal.datosprueba.dominio.excepcion;

/** R3 y R15: una corrida a la vez, y no se da de baja mientras corre. */
public class GeneracionEnCursoException extends DatosPruebaException {
    public GeneracionEnCursoException() {
        super("Ya se están generando datos de prueba. Espera a que termine (puedes ver el avance)");
    }
}
