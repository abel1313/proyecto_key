package com.ventas.key.hexagonal.busquedapedido.dominio.modelo;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Los estados como los dice la card de Mis pedidos (R3), no como los guarda la base:
 * <ul>
 *   <li>{@code PENDIENTE}: contado que todavia no se cobra ({@code estado_pedido = 'Pendiente'}).</li>
 *   <li>{@code POR_COBRAR}: Apartado o Ir pagando abierto ({@code estado_pedido} = su tipo).</li>
 *   <li>{@code PAGADO}: Apartado o Ir pagando liquidado ({@code 'PAGADO'}).</li>
 *   <li>{@code ENTREGADO}: contado cobrado ({@code 'Entregado'}).</li>
 *   <li>{@code CANCELADO}: {@code 'cancelado'}, se guarda en minuscula.</li>
 * </ul>
 */
public enum EstadoBuscado {
    PENDIENTE,
    POR_COBRAR,
    PAGADO,
    ENTREGADO,
    CANCELADO
}
