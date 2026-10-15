package com.ventas.key.hexagonal.busquedapedido.dominio.modelo;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Filtro de pedidos unidos (R9). De un grupo activo solo sale el titular, que en la card ya
 * muestra el total de todos.
 */
public enum PedidosUnidos {
    SOLO_UNIDOS,
    SIN_UNIR
}
