package com.ventas.key.hexagonal.busquedapedido.dominio.modelo;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Como se ordena la lista (R11). Siempre se desempata por numero de pedido, para que al pasar de
 * pagina no se repita ni se brinque ninguno.
 */
public enum OrdenDePedidos {
    /** El mas nuevo primero (por fecha y hora de registro). Es el de siempre. */
    RECIENTES,
    ANTIGUOS,
    /** La fecha de entrega o recogida mas cercana primero; los que no tienen fecha, al final. */
    ENTREGA_PROXIMA,
    /** El que mas debe primero. */
    MAYOR_SALDO
}
