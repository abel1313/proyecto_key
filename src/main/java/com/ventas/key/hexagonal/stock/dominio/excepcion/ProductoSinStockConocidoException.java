package com.ventas.key.hexagonal.stock.dominio.excepcion;

/**
 * Se pidio la disponibilidad de un producto que no existe.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 */
public class ProductoSinStockConocidoException extends RuntimeException {

    public ProductoSinStockConocidoException(Integer productoId) {
        super("No existe el producto " + productoId + ", no se puede calcular su stock disponible");
    }
}
