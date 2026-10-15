package com.ventas.key.hexagonal.busquedapedido.dominio.modelo;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Si el cliente recoge en la tienda (sin lugar, o un lugar marcado "recoger en tienda") o se le
 * lleva a un lugar de entrega (R8).
 */
public enum ModoDeEntrega {
    RECOGE_EN_TIENDA,
    ENVIO
}
