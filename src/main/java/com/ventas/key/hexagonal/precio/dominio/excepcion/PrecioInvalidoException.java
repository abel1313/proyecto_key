package com.ventas.key.hexagonal.precio.dominio.excepcion;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>RuntimeException: {@code ExceptionGlobal} la devuelve como 400 con este mensaje.
 */
public class PrecioInvalidoException extends RuntimeException {

    private PrecioInvalidoException(String mensaje) {
        super(mensaje);
    }

    public static PrecioInvalidoException ventaObligatoria() {
        return new PrecioInvalidoException("El precio normal es obligatorio y debe ser mayor a 0");
    }

    public static PrecioInvalidoException rebajaNegativa() {
        return new PrecioInvalidoException("El precio con descuento no puede ser negativo. Déjalo en 0 si no lleva descuento");
    }

    public static PrecioInvalidoException rebajaMayorQueVenta(double rebaja, double venta) {
        return new PrecioInvalidoException(String.format(
                "El precio con descuento ($%.2f) no puede ser mayor al normal ($%.2f). "
                        + "Si quieres cobrar más, sube el precio normal", rebaja, venta));
    }

    public static PrecioInvalidoException productoNoExiste(Integer productoId) {
        return new PrecioInvalidoException("No existe el producto " + productoId);
    }
}
