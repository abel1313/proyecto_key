package com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.ArticuloDisponible;

/**
 * No alcanza el stock para meter ese articulo en el pedido (R3).
 *
 * <p>Dice cuanto hay en el articulo y cuanto en el modelo por separado: cuando el que se queda
 * corto es el modelo, mirar solo el stock de la talla no explica nada.
 */
public class StockInsuficienteException extends EdicionPedidoException {

    public StockInsuficienteException(ArticuloDisponible articulo, int cantidad) {
        super("No hay stock suficiente de '" + articulo.nombre() + "'. "
                + articulo.detalleDelFaltante(cantidad));
    }
}
