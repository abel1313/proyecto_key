package com.ventas.key.hexagonal.precio.dominio.puerto.salida;

import com.ventas.key.hexagonal.precio.dominio.modelo.PreciosDeProducto;

import java.util.Optional;

/** [Hexagonal: Driven Port] [Clean: Interface Adapter] */
public interface PreciosProductoPort {

    Optional<PreciosDeProducto> buscar(Integer productoId);

    void guardar(PreciosDeProducto precios);
}
