package com.ventas.key.hexagonal.datosprueba.dominio.excepcion;

/** R1: el generador solo corre en la base de QA. */
public class AmbienteNoPermitidoException extends DatosPruebaException {
    public AmbienteNoPermitidoException(String baseActual) {
        super("Los datos de prueba solo se pueden generar en la base de QA (inventario_key_qa). "
                + "Esta base es '" + baseActual + "': no se hizo nada");
    }
}
