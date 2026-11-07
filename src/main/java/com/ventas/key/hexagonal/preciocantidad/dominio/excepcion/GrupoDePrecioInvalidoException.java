package com.ventas.key.hexagonal.preciocantidad.dominio.excepcion;

/** Una regla del grupo de precio por cantidad no se cumple. El mensaje va en palabras del dueño. */
public class GrupoDePrecioInvalidoException extends RuntimeException {
    public GrupoDePrecioInvalidoException(String mensaje) {
        super(mensaje);
    }
}
