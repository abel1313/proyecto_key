package com.ventas.key.hexagonal.busquedapedido.dominio.modelo;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Filtros de dinero (R4). Solo los Apartados y los Ir pagando llevan abonos, asi que
 * {@code CON_SALDO} y {@code SIN_ABONOS} se refieren a ellos:
 * <ul>
 *   <li>{@code CON_SALDO}: Apartado o Ir pagando abierto que todavia debe algo.</li>
 *   <li>{@code SIN_ABONOS}: Apartado o Ir pagando abierto sin ningun abono registrado.</li>
 *   <li>{@code SALDO_A_FAVOR}: el cliente pago mas de lo que vale el pedido (le quitaron
 *       articulos), o se cancelo un Apartado con dinero o un pedido ya pagado (devolucion). Es
 *       dinero que hay que devolverle. Un Ir pagando cancelado que todavia debia <b>no</b>: se llevo
 *       la mercancia y lo que falto es deuda incobrable (AbonoServiceImpl.cancelarPedido).</li>
 * </ul>
 */
public enum SituacionDeDinero {
    CON_SALDO,
    SIN_ABONOS,
    SALDO_A_FAVOR
}
