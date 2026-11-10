package com.ventas.key.hexagonal.preciocantidad.dominio.puerto.entrada;

import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.GrupoPrecioCantidad;

import java.util.List;
import java.util.Optional;

/** [Hexagonal: Driving Port] Lo que el administrador hace con los grupos de precio por cantidad. */
public interface AdministrarGruposCasoUso {

    List<GrupoPrecioCantidad> listar();

    GrupoPrecioCantidad obtener(int id);

    GrupoPrecioCantidad crear(GrupoPrecioCantidad grupo);

    GrupoPrecioCantidad actualizar(int id, GrupoPrecioCantidad grupo);

    void eliminar(int id);

    void cambiarActivo(int id, boolean activo);

    /** El grupo al que pertenece un producto (para mostrarlo en Agregar y Actualizar producto). */
    Optional<GrupoPrecioCantidad> grupoDelProducto(int productoId);
}
