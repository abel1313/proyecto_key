package com.ventas.key.hexagonal.preciocantidad.dominio.puerto.salida;

import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.GrupoPrecioCantidad;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** [Hexagonal: Driven Port] [Clean: Interface Adapter] Lo que el dominio necesita para guardar y leer grupos. */
public interface GruposDePrecioPort {

    List<GrupoPrecioCantidad> listar();

    Optional<GrupoPrecioCantidad> buscar(int id);

    /** Los grupos activos que incluyen alguno de estos productos (las fechas las revisa el dominio). */
    List<GrupoPrecioCantidad> activosConProductos(Set<Integer> productoIds);

    /** El grupo en el que ya está el producto, si está en alguno. */
    Optional<GrupoPrecioCantidad> grupoDelProducto(int productoId);

    /** Crea (id {@code null}) o actualiza. Rechaza con {@code GrupoDePrecioInvalidoException} un producto que ya está en otro grupo o que no existe. */
    GrupoPrecioCantidad guardar(GrupoPrecioCantidad grupo);

    void eliminar(int id);

    void cambiarActivo(int id, boolean activo);
}
