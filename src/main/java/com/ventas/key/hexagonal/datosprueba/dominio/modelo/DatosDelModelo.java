package com.ventas.key.hexagonal.datosprueba.dominio.modelo;

/** Cómo se encuentra en pantalla el modelo de un artículo: su número, su nombre y su código de barras. */
public record DatosDelModelo(int modeloId, String nombre, String codigoBarras) {
}
