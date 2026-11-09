package com.ventas.key.hexagonal.calculoprecio.dominio.excepcion;

/** El producto al que se quiere ligar el cálculo no existe (por ejemplo, no se guardó todavía). */
public class ProductoSinCalculoException extends RuntimeException {
    public ProductoSinCalculoException(int productoId) {
        super("No existe el producto " + productoId + ": guarda primero el producto y luego su cálculo de precio.");
    }
}
