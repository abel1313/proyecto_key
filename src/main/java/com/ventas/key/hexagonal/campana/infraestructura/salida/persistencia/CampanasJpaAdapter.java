package com.ventas.key.hexagonal.campana.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.campana.dominio.excepcion.CampanaInvalidaException;
import com.ventas.key.hexagonal.campana.dominio.modelo.Campana;
import com.ventas.key.hexagonal.campana.dominio.modelo.PrecioDeCampana;
import com.ventas.key.hexagonal.campana.dominio.puerto.salida.CampanasPort;
import com.ventas.key.hexagonal.preciocantidad.infraestructura.salida.persistencia.TablasDisponibles;
import com.ventas.key.mis.productos.entity.CampanaEntidad;
import com.ventas.key.mis.productos.entity.CampanaProductoEntidad;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.repository.ICampanaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** [Hexagonal: Driven Adapter] [Clean: Interface Adapter] Guarda y lee las campañas en MySQL. */
@Component
public class CampanasJpaAdapter implements CampanasPort {

    private final ICampanaRepository repositorio;
    private final TablasDisponibles tablas;

    @PersistenceContext
    private EntityManager em;

    public CampanasJpaAdapter(ICampanaRepository repositorio, TablasDisponibles tablas) {
        this.repositorio = repositorio;
        this.tablas = tablas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Campana> listar() {
        return repositorio.listarConProductos().stream().map(this::aDominio).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Campana> buscar(int id) {
        return repositorio.buscarConProductos(id).map(this::aDominio);
    }

    /** Sin la migración no hay campañas: la venta sigue igual y no se consulta una tabla inexistente dentro de su transacción. */
    @Override
    @Transactional(readOnly = true)
    public List<Campana> activas() {
        if (!tablas.existe("campana") || !tablas.existe("campana_producto")) {
            return List.of();
        }
        return repositorio.activasConProductos().stream().map(this::aDominio).toList();
    }

    @Override
    @Transactional
    public Campana guardar(Campana c) {
        CampanaEntidad e = c.id() == null
                ? new CampanaEntidad()
                : repositorio.buscarConProductos(c.id()).orElseThrow(() -> new CampanaInvalidaException("La campaña ya no existe."));
        if (c.id() == null) {
            e.setFechaCreacion(LocalDateTime.now());
        }
        e.setNombre(c.nombre());
        e.setFechaInicio(c.fechaInicio());
        e.setFechaFin(c.fechaFin());
        e.setActiva(c.activa());
        e.setMetaMonto(c.metaMonto());

        e.getProductos().clear();
        if (e.getId() != null) {
            repositorio.saveAndFlush(e);
        }
        for (PrecioDeCampana p : c.precios()) {
            Producto producto = em.find(Producto.class, p.productoId());
            if (producto == null) {
                throw new CampanaInvalidaException("El producto " + (p.nombre() == null ? p.productoId() : "«" + p.nombre() + "»") + " ya no existe.");
            }
            if (Boolean.TRUE.equals(producto.getEsCatalogoInterno())) {
                // Un producto «sombra» (flores eternas) solo existe para ser línea de un pedido: no se ofrece como juguete de campaña.
                throw new CampanaInvalidaException("El producto «" + producto.getNombre() + "» es interno y no puede estar en una campaña.");
            }
            CampanaProductoEntidad fila = new CampanaProductoEntidad();
            fila.setCampana(e);
            fila.setProducto(producto);
            fila.setPrecioCampana(p.precio());
            e.getProductos().add(fila);
        }
        return aDominio(repositorio.saveAndFlush(e));
    }

    @Override
    @Transactional
    public void eliminar(int id) {
        repositorio.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tieneComprasLigadas(int id) {
        if (!tablas.existe("campana_persona_pedido")) {
            return false;
        }
        Number n = (Number) em.createNativeQuery("SELECT COUNT(*) FROM campana_persona_pedido WHERE campana_id = :id")
                .setParameter("id", id).getSingleResult();
        return n != null && n.longValue() > 0;
    }

    @Override
    @Transactional
    public void activarSola(int id) {
        CampanaEntidad e = repositorio.findById(id).orElseThrow(() -> new CampanaInvalidaException("La campaña ya no existe."));
        repositorio.apagarLasDemas(id);
        e.setActiva(true);
        repositorio.save(e);
    }

    @Override
    @Transactional
    public void apagar(int id) {
        CampanaEntidad e = repositorio.findById(id).orElseThrow(() -> new CampanaInvalidaException("La campaña ya no existe."));
        e.setActiva(false);
        repositorio.save(e);
    }

    private Campana aDominio(CampanaEntidad e) {
        List<PrecioDeCampana> precios = e.getProductos().stream()
                .map(f -> new PrecioDeCampana(f.getProducto().getId(), f.getProducto().getNombre(), f.getPrecioCampana()))
                .toList();
        return new Campana(e.getId(), e.getNombre(), e.getFechaInicio(), e.getFechaFin(), e.isActiva(), e.getMetaMonto(), precios);
    }
}
