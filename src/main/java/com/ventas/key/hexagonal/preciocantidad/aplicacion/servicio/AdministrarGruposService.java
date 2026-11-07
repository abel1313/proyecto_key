package com.ventas.key.hexagonal.preciocantidad.aplicacion.servicio;

import com.ventas.key.hexagonal.preciocantidad.dominio.excepcion.GrupoDePrecioInvalidoException;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.GrupoPrecioCantidad;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.MiembroDeGrupo;
import com.ventas.key.hexagonal.preciocantidad.dominio.puerto.entrada.AdministrarGruposCasoUso;
import com.ventas.key.hexagonal.preciocantidad.dominio.puerto.salida.GruposDePrecioPort;

import java.util.List;
import java.util.Optional;

/** [Clean: Use Case] Crear, cambiar, apagar y borrar grupos. Cuida G2: un producto está en un solo grupo. Java puro. */
public class AdministrarGruposService implements AdministrarGruposCasoUso {

    private final GruposDePrecioPort grupos;

    public AdministrarGruposService(GruposDePrecioPort grupos) {
        this.grupos = grupos;
    }

    @Override
    public List<GrupoPrecioCantidad> listar() {
        return grupos.listar();
    }

    @Override
    public GrupoPrecioCantidad obtener(int id) {
        return grupos.buscar(id).orElseThrow(() -> new GrupoDePrecioInvalidoException("El grupo ya no existe."));
    }

    @Override
    public GrupoPrecioCantidad crear(GrupoPrecioCantidad grupo) {
        exigirProductosLibres(grupo, null);
        return grupos.guardar(conId(grupo, null));
    }

    @Override
    public GrupoPrecioCantidad actualizar(int id, GrupoPrecioCantidad grupo) {
        obtener(id);
        exigirProductosLibres(grupo, id);
        return grupos.guardar(conId(grupo, id));
    }

    @Override
    public void eliminar(int id) {
        obtener(id);
        grupos.eliminar(id);
    }

    @Override
    public void cambiarActivo(int id, boolean activo) {
        obtener(id);
        grupos.cambiarActivo(id, activo);
    }

    @Override
    public Optional<GrupoPrecioCantidad> grupoDelProducto(int productoId) {
        return grupos.grupoDelProducto(productoId);
    }

    private void exigirProductosLibres(GrupoPrecioCantidad grupo, Integer idPropio) {
        if (grupo.miembros().isEmpty()) {
            throw new GrupoDePrecioInvalidoException("El grupo necesita al menos un producto.");
        }
        for (MiembroDeGrupo m : grupo.miembros()) {
            grupos.grupoDelProducto(m.productoId())
                    .filter(otro -> idPropio == null || !idPropio.equals(otro.id()))
                    .ifPresent(otro -> {
                        throw new GrupoDePrecioInvalidoException("«" + (m.nombre() == null ? "El producto " + m.productoId() : m.nombre())
                                + "» ya está en el grupo «" + otro.nombre() + "». Un producto solo puede estar en un grupo.");
                    });
        }
    }

    private static GrupoPrecioCantidad conId(GrupoPrecioCantidad g, Integer id) {
        return new GrupoPrecioCantidad(id, g.nombre(), g.minimoPiezas(), g.fechaInicio(), g.fechaFin(), g.activo(), g.miembros());
    }
}
