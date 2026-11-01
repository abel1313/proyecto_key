package com.ventas.key.hexagonal.revisionventas.dominio.modelo;

import java.util.List;

/** Lo que devuelve el puerto por cada tipo: el total real y solo los primeros ejemplos. */
public record BusquedaDeHallazgos(int total, List<Hallazgo> ejemplos) {
}
