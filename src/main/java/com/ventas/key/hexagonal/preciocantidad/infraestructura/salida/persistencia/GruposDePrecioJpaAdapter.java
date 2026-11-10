package com.ventas.key.hexagonal.preciocantidad.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.preciocantidad.dominio.excepcion.GrupoDePrecioInvalidoException;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.GrupoPrecioCantidad;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.MiembroDeGrupo;
import com.ventas.key.hexagonal.preciocantidad.dominio.puerto.salida.GruposDePrecioPort;
import com.ventas.key.mis.productos.entity.GrupoPrecioCantidadEntidad;
import com.ventas.key.mis.productos.entity.GrupoPrecioCantidadProductoEntidad;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.repository.IGrupoPrecioCantidadRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** [Hexagonal: Driven Adapter] [Clean: Interface Adapter] Guarda y lee los grupos en MySQL. */
@Component
public class GruposDePrecioJpaAdapter implements GruposDePrecioPort {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GruposDePrecioJpaAdapter.class);
    /** Cada cuánto se vuelve a mirar si ya existen las tablas, mientras no existan (así, al correr la migración, empieza sola). */
    private static final long REVISAR_CADA_MS = 60_000L;

    private final IGrupoPrecioCantidadRepository repositorio;
    private final DataSource dataSource;

    @PersistenceContext
    private EntityManager em;

    private volatile boolean tablasListas;
    private volatile long ultimaRevision;

    public GruposDePrecioJpaAdapter(IGrupoPrecioCantidadRepository repositorio, DataSource dataSource) {
        this.repositorio = repositorio;
        this.dataSource = dataSource;
    }

    /**
     * ¿Ya corrió {@code migration_grupo_precio_cantidad.sql} en esta base? Se pregunta por una conexión APARTE de la venta: si la tabla no
     * existe y la consulta fallara dentro de la transacción de la venta, la marcaría para deshacerse y la venta entera se caería. Así, en un
     * ambiente sin la migración, el precio por cantidad simplemente no aplica y las ventas siguen como siempre.
     */
    boolean tablasDisponibles() {
        if (tablasListas) {
            return true;
        }
        long ahora = System.currentTimeMillis();
        if (ultimaRevision != 0 && ahora - ultimaRevision < REVISAR_CADA_MS) {
            return false;
        }
        ultimaRevision = ahora;
        try (Connection c = dataSource.getConnection();
             ResultSet rs = c.getMetaData().getTables(c.getCatalog(), null, "%", new String[]{"TABLE"})) {
            boolean grupo = false;
            boolean producto = false;
            while (rs.next()) {
                String nombre = rs.getString("TABLE_NAME");
                grupo = grupo || "grupo_precio_cantidad".equalsIgnoreCase(nombre);
                producto = producto || "grupo_precio_cantidad_producto".equalsIgnoreCase(nombre);
            }
            tablasListas = grupo && producto;
        } catch (SQLException e) {
            log.warn("No se pudo revisar si existen las tablas de grupos de precio: {}", e.getMessage());
            tablasListas = false;
        }
        if (!tablasListas) {
            log.warn("Las tablas de grupos de precio por cantidad no existen: no se aplica el precio por cantidad (falta migration_grupo_precio_cantidad.sql)");
        }
        return tablasListas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GrupoPrecioCantidad> listar() {
        return repositorio.listarConProductos().stream().map(this::aDominio).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GrupoPrecioCantidad> buscar(int id) {
        return repositorio.buscarConProductos(id).map(this::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GrupoPrecioCantidad> activosConProductos(Set<Integer> productoIds) {
        if (productoIds == null || productoIds.isEmpty() || !tablasDisponibles()) {
            return List.of();
        }
        return repositorio.activosConProductos(productoIds).stream().map(this::aDominio).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GrupoPrecioCantidad> grupoDelProducto(int productoId) {
        return repositorio.deProducto(productoId).map(this::aDominio);
    }

    @Override
    @Transactional
    public GrupoPrecioCantidad guardar(GrupoPrecioCantidad g) {
        GrupoPrecioCantidadEntidad e = g.id() == null
                ? new GrupoPrecioCantidadEntidad()
                : repositorio.buscarConProductos(g.id()).orElseThrow(() -> new GrupoDePrecioInvalidoException("El grupo ya no existe."));
        if (g.id() == null) {
            e.setFechaCreacion(LocalDateTime.now());
        }
        e.setNombre(g.nombre());
        e.setMinimoPiezas(g.minimoPiezas());
        e.setFechaInicio(g.fechaInicio());
        e.setFechaFin(g.fechaFin());
        e.setActivo(g.activo());

        // Se vacía y se rearma la lista de productos; el flush intermedio evita chocar con el único de producto_id al moverlos.
        e.getProductos().clear();
        if (e.getId() != null) {
            repositorio.saveAndFlush(e);
        }
        for (MiembroDeGrupo m : g.miembros()) {
            Producto producto = em.find(Producto.class, m.productoId());
            if (producto == null) {
                throw new GrupoDePrecioInvalidoException("El producto " + (m.nombre() == null ? m.productoId() : "«" + m.nombre() + "»") + " ya no existe.");
            }
            GrupoPrecioCantidadProductoEntidad fila = new GrupoPrecioCantidadProductoEntidad();
            fila.setGrupo(e);
            fila.setProducto(producto);
            fila.setPrecioPromocion(m.precioPromocion());
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
    @Transactional
    public void cambiarActivo(int id, boolean activo) {
        GrupoPrecioCantidadEntidad e = repositorio.findById(id).orElseThrow(() -> new GrupoDePrecioInvalidoException("El grupo ya no existe."));
        e.setActivo(activo);
        repositorio.save(e);
    }

    private GrupoPrecioCantidad aDominio(GrupoPrecioCantidadEntidad e) {
        List<MiembroDeGrupo> miembros = e.getProductos().stream()
                .map(f -> new MiembroDeGrupo(f.getProducto().getId(), f.getProducto().getNombre(), f.getPrecioPromocion()))
                .toList();
        return new GrupoPrecioCantidad(e.getId(), e.getNombre(), e.getMinimoPiezas(), e.getFechaInicio(), e.getFechaFin(), e.isActivo(),
                miembros);
    }
}
