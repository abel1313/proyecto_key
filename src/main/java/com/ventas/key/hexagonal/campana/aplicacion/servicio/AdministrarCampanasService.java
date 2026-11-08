package com.ventas.key.hexagonal.campana.aplicacion.servicio;

import com.ventas.key.hexagonal.campana.dominio.excepcion.CampanaInvalidaException;
import com.ventas.key.hexagonal.campana.dominio.modelo.Campana;
import com.ventas.key.hexagonal.campana.dominio.puerto.entrada.AdministrarCampanasCasoUso;
import com.ventas.key.hexagonal.campana.dominio.puerto.salida.CampanasPort;

import java.util.List;

/** [Clean: Use Case] Crear, cambiar, encender, apagar, duplicar y borrar campañas. Solo una encendida a la vez. Java puro. */
public class AdministrarCampanasService implements AdministrarCampanasCasoUso {

    private final CampanasPort campanas;

    public AdministrarCampanasService(CampanasPort campanas) {
        this.campanas = campanas;
    }

    @Override
    public List<Campana> listar() {
        return campanas.listar();
    }

    @Override
    public Campana obtener(int id) {
        return campanas.buscar(id).orElseThrow(() -> new CampanaInvalidaException("La campaña ya no existe."));
    }

    @Override
    public Campana crear(Campana c) {
        exigirProductos(c);
        // Una campaña nueva nace apagada: se enciende a propósito con «Activar», que apaga las demás.
        return campanas.guardar(conIdYEstado(c, null, false));
    }

    @Override
    public Campana actualizar(int id, Campana c) {
        Campana actual = obtener(id);
        exigirProductos(c);
        return campanas.guardar(conIdYEstado(c, id, actual.activa()));
    }

    @Override
    public void eliminar(int id) {
        obtener(id);
        campanas.eliminar(id);
    }

    @Override
    public Campana activar(int id) {
        Campana c = obtener(id);
        exigirProductos(c);
        campanas.activarSola(id);
        return obtener(id);
    }

    @Override
    public Campana apagar(int id) {
        obtener(id);
        campanas.apagar(id);
        return obtener(id);
    }

    @Override
    public Campana duplicar(int id) {
        Campana c = obtener(id);
        return campanas.guardar(new Campana(null, "Copia de " + c.nombre(), c.fechaInicio(), c.fechaFin(), false, c.metaMonto(), c.precios()));
    }

    private static void exigirProductos(Campana c) {
        if (c.precios().isEmpty()) {
            throw new CampanaInvalidaException("La campaña necesita al menos un juguete con su precio.");
        }
    }

    private static Campana conIdYEstado(Campana c, Integer id, boolean activa) {
        return new Campana(id, c.nombre(), c.fechaInicio(), c.fechaFin(), activa, c.metaMonto(), c.precios());
    }
}
