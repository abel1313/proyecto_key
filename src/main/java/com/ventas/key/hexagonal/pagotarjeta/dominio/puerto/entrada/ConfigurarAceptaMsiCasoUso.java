package com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.ConfigMsiArticulo;

import java.util.Collection;
import java.util.Map;

/** [Hexagonal: Driving Port] [Clean: Use Case Input Boundary] */
public interface ConfigurarAceptaMsiCasoUso {

    boolean aceptaProducto(Integer productoId);

    void cambiarProducto(Integer productoId, boolean acepta);

    ConfigMsiArticulo articulo(Integer varianteId);

    /** @param acepta null = que el artículo herede la casilla de su producto */
    void cambiarArticulo(Integer varianteId, Boolean acepta);

    /** Valor efectivo de cada artículo (para pintar el chip "MSI" en las listas). */
    Map<Integer, Boolean> efectivos(Collection<Integer> varianteIds);
}
