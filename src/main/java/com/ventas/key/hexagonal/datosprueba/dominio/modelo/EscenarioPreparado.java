package com.ventas.key.hexagonal.datosprueba.dominio.modelo;

import java.util.List;

/** Lo que quedo listo para una prueba y lo que hay que hacer despues. */
public record EscenarioPreparado(String prueba, String titulo, List<PedidoPreparado> pedidos, String siguientePaso) {
}
