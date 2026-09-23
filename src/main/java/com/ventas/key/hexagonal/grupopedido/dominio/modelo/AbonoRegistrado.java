package com.ventas.key.hexagonal.grupopedido.dominio.modelo;

import java.time.LocalDate;

/**
 * Un abono tal como esta guardado, para poder moverlo de pedido al separar.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 */
public record AbonoRegistrado(Integer abonoId, Integer pedidoId, long centavos, LocalDate fecha) {
}
