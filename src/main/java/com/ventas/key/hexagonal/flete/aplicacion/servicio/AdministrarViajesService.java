package com.ventas.key.hexagonal.flete.aplicacion.servicio;

import com.ventas.key.hexagonal.flete.dominio.excepcion.ViajeInvalidoException;
import com.ventas.key.hexagonal.flete.dominio.modelo.ViajeDeCompra;
import com.ventas.key.hexagonal.flete.dominio.puerto.entrada.AdministrarViajesCasoUso;
import com.ventas.key.hexagonal.flete.dominio.puerto.salida.ViajesPort;

import java.util.List;

/** [Hexagonal: Application] [Clean: Use Case] Orquesta; las reglas viven en {@link ViajeDeCompra}. Java puro. */
public class AdministrarViajesService implements AdministrarViajesCasoUso {

    private final ViajesPort viajes;

    public AdministrarViajesService(ViajesPort viajes) {
        this.viajes = viajes;
    }

    @Override
    public List<ViajeDeCompra> listar() {
        return viajes.listar();
    }

    @Override
    public ViajeDeCompra obtener(int id) {
        return viajes.buscar(id).orElseThrow(() -> new ViajeInvalidoException("Ese viaje ya no existe."));
    }

    @Override
    public List<ViajeDeCompra> deProducto(int productoId) {
        return viajes.deProducto(productoId);
    }

    @Override
    public ViajeDeCompra crear(ViajeDeCompra viaje) {
        return viajes.guardar(viaje.conId(null));
    }

    @Override
    public ViajeDeCompra actualizar(int id, ViajeDeCompra viaje) {
        obtener(id);
        return viajes.guardar(viaje.conId(id));
    }

    @Override
    public void eliminar(int id) {
        obtener(id);
        viajes.eliminar(id);
    }
}
