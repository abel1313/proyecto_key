package com.ventas.key.hexagonal.datosprueba.infraestructura.dto;

/**
 * Lo que manda el boton. Todo es opcional: sin nada genera 20 000 modelos de 1 a 4 articulos y
 * 1 000 pedidos.
 */
public record GenerarDatosPruebaRequest(Integer modelos, Integer articulosMin, Integer articulosMax,
                                        Integer pedidos, Long semilla) {
}
