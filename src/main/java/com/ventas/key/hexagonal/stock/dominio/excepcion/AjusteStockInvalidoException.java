package com.ventas.key.hexagonal.stock.dominio.excepcion;

/**
 * Un ajuste de stock del modelo que rompe una regla: 0, deja el modelo en negativo o por debajo de
 * lo que ya tienen repartido sus articulos. El mensaje se muestra tal cual en pantalla.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 */
public class AjusteStockInvalidoException extends RuntimeException {

    public AjusteStockInvalidoException(String mensaje) {
        super(mensaje);
    }
}
