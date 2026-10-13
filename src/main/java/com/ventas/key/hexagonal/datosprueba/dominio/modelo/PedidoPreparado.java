package com.ventas.key.hexagonal.datosprueba.dominio.modelo;

/**
 * Un pedido que se creo para una prueba: el nombre que le da la prueba (P1, PE...), su numero, que es
 * (en palabras de la pantalla) y el nombre con el que se ve su cliente en pantalla
 * ({@link NombresDePrueba#cliente}: "Prueba 24 K4"), que es lo que se busca en Mis pedidos.
 */
public record PedidoPreparado(String clave, int pedidoId, String queEs, String cliente) {
}
