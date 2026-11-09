package com.ventas.key.hexagonal.calculoprecio.dominio.excepcion;

/** [Clean: Entity] Un dato del cálculo de precio no cumple las reglas. El mensaje está en palabras del dueño. */
public class CalculoPrecioInvalidoException extends RuntimeException {
    public CalculoPrecioInvalidoException(String mensaje) {
        super(mensaje);
    }
}
