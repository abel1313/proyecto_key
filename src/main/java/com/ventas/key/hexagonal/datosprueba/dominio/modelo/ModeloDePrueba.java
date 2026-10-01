package com.ventas.key.hexagonal.datosprueba.dominio.modelo;

import java.util.List;

/**
 * Un modelo (producto) de prueba con sus articulos. El stock del modelo es la suma del de sus
 * articulos (R6): asi el "stock libre para armar modelos" queda en 0, como en un alta normal.
 */
public record ModeloDePrueba(
        String codigoBarras,
        String nombre,
        String descripcion,
        String categoria,
        String color,
        double precioCosto,
        double precioVenta,
        double precioRebaja,
        List<ArticuloDePrueba> articulos) {

    public static final String MARCA = "Prueba QA";

    public int stock() {
        return articulos.stream().mapToInt(ArticuloDePrueba::stock).sum();
    }
}
