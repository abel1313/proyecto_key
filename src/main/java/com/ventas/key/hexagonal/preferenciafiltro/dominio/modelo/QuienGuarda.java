package com.ventas.key.hexagonal.preferenciafiltro.dominio.modelo;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>El usuario de la petición, reducido a lo que las reglas necesitan saber (R1, R3).
 */
public record QuienGuarda(int usuarioId, boolean esPersonal) {
}
