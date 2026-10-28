package com.ventas.key.hexagonal.pagotarjeta.infraestructura.entrada.rest.dto;

/**
 * Lo que manda el front para cambiar la casilla de meses sin intereses.
 *
 * @param aceptaMsi en un producto debe ser true o false; en un artículo, null significa "heredar del producto"
 */
public record AceptaMsiRequest(Boolean aceptaMsi) {
}
