package com.ventas.key.hexagonal.datosprueba.dominio.modelo;

/**
 * Un pedido que se creo para una prueba: el nombre que le da la prueba (P1, PE...), su numero y
 * que es, en palabras de la pantalla.
 */
public record PedidoPreparado(String clave, int pedidoId, String queEs) {
}
