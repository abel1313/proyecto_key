package com.ventas.key.hexagonal.precio.dominio.puerto.salida;

import com.ventas.key.hexagonal.precio.dominio.modelo.PreciosDeArticulo;

import java.util.Optional;

/** [Hexagonal: Driven Port] [Clean: Interface Adapter] */
public interface PreciosArticuloPort {

    /** Los precios con que se vende hoy el articulo: los propios, o los del producto si no tiene. */
    Optional<PreciosDeArticulo> buscar(Integer varianteId);

    /** Con {@code propio} false borra el precio propio y el articulo vuelve al del producto. */
    void guardar(PreciosDeArticulo precios);
}
