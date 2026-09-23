package com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida;

/**
 * Mover stock al editar un pedido.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 *
 * <p>Dos metodos y nada mas. El sistema descuenta del articulo <b>y</b> de su modelo, asi que el
 * adaptador toca los dos -- pero eso es un detalle del modelo de datos, no algo que el dominio
 * tenga que saber.
 */
public interface MovimientoStockPort {

    /** Saca del inventario lo que se lleva el pedido. */
    void descontar(Integer varianteId, int cantidad);

    /** Devuelve al inventario lo que sale del pedido. */
    void devolver(Integer varianteId, int cantidad);
}
