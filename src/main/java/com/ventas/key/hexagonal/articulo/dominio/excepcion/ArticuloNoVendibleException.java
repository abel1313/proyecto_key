package com.ventas.key.hexagonal.articulo.dominio.excepcion;

/**
 * El articulo no se puede vender: esta deshabilitado (el o su producto) o no alcanza el stock.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Es un RuntimeException: {@code ExceptionGlobal} lo devuelve como 400 con este mensaje, igual
 * que los rechazos de stock que ya existian en pedidos y ventas.
 */
public class ArticuloNoVendibleException extends RuntimeException {

    private ArticuloNoVendibleException(String mensaje) {
        super(mensaje);
    }

    public static ArticuloNoVendibleException deshabilitado(String nombre, String quien) {
        return new ArticuloNoVendibleException(String.format(
                "'%s' ya no está a la venta: %s está deshabilitado o dado de baja. "
                        + "Quítalo del carrito para continuar.", nombre, quien));
    }

    public static ArticuloNoVendibleException sinStock(String nombre, int disponible, int solicitado) {
        return new ArticuloNoVendibleException(String.format(
                "No hay suficiente stock de '%s'. Disponible: %d, solicitado: %d",
                nombre, disponible, solicitado));
    }
}
