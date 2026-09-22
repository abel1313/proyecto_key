package com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.PrecioCatalogo;

/**
 * Se intento cobrar un monto que no es ninguno de los dos precios del catalogo (R2).
 *
 * <p>El mensaje dice cuales si valen: sin eso, quien lo recibe no tiene forma de corregirlo salvo
 * adivinando.
 */
public class PrecioNoCobrableException extends EdicionPedidoException {

    public PrecioNoCobrableException(PrecioCatalogo precios, Double intentado) {
        super(String.format("El precio $%.2f no es valido para '%s'. Se puede cobrar a %s",
                intentado == null ? 0.0 : intentado, precios.nombreProducto(), precios.explicacion()));
    }
}
