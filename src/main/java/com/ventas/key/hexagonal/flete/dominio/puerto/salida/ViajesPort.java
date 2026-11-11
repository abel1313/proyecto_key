package com.ventas.key.hexagonal.flete.dominio.puerto.salida;

import com.ventas.key.hexagonal.flete.dominio.modelo.ViajeDeCompra;

import java.util.List;
import java.util.Optional;

/** [Hexagonal: Driven Port] Lo que el dominio necesita para guardar y leer viajes de compra. */
public interface ViajesPort {
    /** Del viaje más reciente al más viejo. */
    List<ViajeDeCompra> listar();

    Optional<ViajeDeCompra> buscar(int id);

    /** Los viajes en los que llegó este producto (del más reciente al más viejo); sirve para avisar que su flete ya está registrado. */
    List<ViajeDeCompra> deProducto(int productoId);

    /** Crea (id {@code null}) o actualiza con TODOS sus gastos y productos. Rechaza un producto que no existe. */
    ViajeDeCompra guardar(ViajeDeCompra viaje);

    void eliminar(int id);
}
