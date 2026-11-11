package com.ventas.key.hexagonal.flete.dominio.puerto.entrada;

import com.ventas.key.hexagonal.flete.dominio.modelo.ViajeDeCompra;

import java.util.List;

/** [Hexagonal: Driving Port] Lo que el administrador hace con los viajes de compra (fletes). */
public interface AdministrarViajesCasoUso {
    List<ViajeDeCompra> listar();

    ViajeDeCompra obtener(int id);

    /** Los viajes en los que llegó este producto (para avisar antes de contar su flete dos veces). */
    List<ViajeDeCompra> deProducto(int productoId);

    ViajeDeCompra crear(ViajeDeCompra viaje);

    /** Reemplaza nombre, fecha, flete, nota y TODA la lista de gastos y productos. */
    ViajeDeCompra actualizar(int id, ViajeDeCompra viaje);

    void eliminar(int id);
}
