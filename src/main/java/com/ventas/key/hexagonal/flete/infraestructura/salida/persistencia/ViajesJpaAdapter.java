package com.ventas.key.hexagonal.flete.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.flete.dominio.excepcion.ViajeInvalidoException;
import com.ventas.key.hexagonal.flete.dominio.modelo.GastoDeViaje;
import com.ventas.key.hexagonal.flete.dominio.modelo.ProductoDelViaje;
import com.ventas.key.hexagonal.flete.dominio.modelo.ViajeDeCompra;
import com.ventas.key.hexagonal.flete.dominio.puerto.salida.ViajesPort;
import com.ventas.key.mis.productos.entity.CompraLoteEntidad;
import com.ventas.key.mis.productos.entity.CompraLoteGastoEntidad;
import com.ventas.key.mis.productos.entity.CompraLoteProductoEntidad;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.repository.ICompraLoteRepository;
import com.ventas.key.hexagonal.preciocantidad.infraestructura.salida.persistencia.TablasDisponibles;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** [Hexagonal: Driven Adapter] [Clean: Interface Adapter] Guarda y lee los viajes de compra en MySQL. */
@Component
public class ViajesJpaAdapter implements ViajesPort {

    private final ICompraLoteRepository repositorio;
    private final TablasDisponibles tablas;

    @PersistenceContext
    private EntityManager em;

    public ViajesJpaAdapter(ICompraLoteRepository repositorio, TablasDisponibles tablas) {
        this.repositorio = repositorio;
        this.tablas = tablas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViajeDeCompra> listar() {
        // Los gastos vienen en la consulta; los productos se leen con los de cada viaje (pocos viajes, tamaño acotado).
        return repositorio.listarConGastos().stream().map(this::aDominio).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ViajeDeCompra> buscar(int id) {
        return repositorio.buscarConGastos(id).map(this::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViajeDeCompra> deProducto(int productoId) {
        return repositorio.deProducto(productoId).stream().map(this::aDominio).toList();
    }

    @Override
    @Transactional
    public ViajeDeCompra guardar(ViajeDeCompra v) {
        CompraLoteEntidad e = v.id() == null
                ? new CompraLoteEntidad()
                : repositorio.buscarConGastos(v.id()).orElseThrow(() -> new ViajeInvalidoException("Ese viaje ya no existe."));
        if (v.id() == null) {
            e.setFechaCreacion(LocalDateTime.now());
        }
        e.setNombre(v.nombre());
        e.setFechaCompra(v.fecha());
        e.setFleteTotal(v.fleteTotal());
        e.setNota(v.nota());

        e.getGastos().clear();
        if (e.getId() != null) {
            repositorio.saveAndFlush(e);   // borra los gastos viejos antes de insertar los nuevos
        }
        for (GastoDeViaje g : v.gastos()) {
            CompraLoteGastoEntidad fila = new CompraLoteGastoEntidad();
            fila.setLote(e);
            fila.setConcepto(g.concepto());
            fila.setMonto(g.monto());
            fila.setCuentaParaCubrir(g.cuentaParaCubrir());
            e.getGastos().add(fila);
        }

        // Los productos se ACTUALIZAN EN SU MISMO RENGLÓN (no se borran y se vuelven a crear): las ventas ligadas a un renglón apuntan a su id.
        Map<Integer, CompraLoteProductoEntidad> actuales = new HashMap<>();
        for (CompraLoteProductoEntidad f : e.getProductos()) {
            actuales.put(f.getProducto().getId(), f);
        }
        Map<Integer, Integer> vendidas = e.getId() == null ? Map.of() : piezasLigadasPorProducto(e.getId());
        Set<Integer> enLaLista = v.productos().stream().map(ProductoDelViaje::productoId).collect(Collectors.toSet());
        for (CompraLoteProductoEntidad f : List.copyOf(e.getProductos())) {
            int ligadas = vendidas.getOrDefault(f.getProducto().getId(), 0);
            if (!enLaLista.contains(f.getProducto().getId())) {
                if (ligadas > 0) {
                    throw new ViajeInvalidoException("No puedes quitar «" + f.getProducto().getNombre() + "» del viaje: ya se vendieron " + ligadas
                            + " pieza(s) de este viaje. Déjalo y, si hace falta, baja su flete.");
                }
                e.getProductos().remove(f);
            }
        }
        for (ProductoDelViaje p : v.productos()) {
            CompraLoteProductoEntidad fila = actuales.get(p.productoId());
            int ligadas = vendidas.getOrDefault(p.productoId(), 0);
            if (fila == null) {
                Producto producto = em.find(Producto.class, p.productoId());
                if (producto == null) {
                    throw new ViajeInvalidoException("El producto " + (p.nombre() == null ? p.productoId() : "«" + p.nombre() + "»") + " ya no existe.");
                }
                fila = new CompraLoteProductoEntidad();
                fila.setLote(e);
                fila.setProducto(producto);
                e.getProductos().add(fila);
            } else if (p.piezas() < ligadas) {
                throw new ViajeInvalidoException("«" + fila.getProducto().getNombre() + "» ya vendió " + ligadas
                        + " pieza(s) de este viaje: no puede tener menos de " + ligadas + " pieza(s).");
            }
            fila.setPiezasCompradas(p.piezas());
            fila.setCostoUnitario(p.costoUnitario());
            fila.setFletePorPieza(p.fletePorPieza());
        }
        return aDominio(repositorio.saveAndFlush(e));
    }

    @Override
    @Transactional
    public void eliminar(int id) {
        int ligadas = piezasLigadasPorProducto(id).values().stream().mapToInt(Integer::intValue).sum();
        if (ligadas > 0) {
            throw new ViajeInvalidoException("Este viaje ya tiene " + ligadas + " pieza(s) vendida(s): no se puede eliminar (se perdería de dónde salió lo vendido).");
        }
        repositorio.deleteById(id);
    }

    /** Piezas ya vendidas (ligadas) de cada producto de este viaje; las ventas devueltas no cuentan. Sin la migración de ventas, no hay nada ligado. */
    private Map<Integer, Integer> piezasLigadasPorProducto(int viajeId) {
        if (!tablas.existe("venta_detalle_lote")) {
            return Map.of();
        }
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createNativeQuery("""
                SELECT clp.producto_id, SUM(vdl.cantidad)
                FROM venta_detalle_lote vdl
                JOIN compra_lote_producto clp ON clp.id = vdl.compra_lote_producto_id
                JOIN detalle_venta_variantes d ON d.id = vdl.detalle_venta_variante_id
                JOIN ventas ve ON ve.id = d.venta_id
                WHERE clp.lote_id = :lote AND COALESCE(ve.estado_venta, '') <> 'Devuelta'
                GROUP BY clp.producto_id
                """).setParameter("lote", viajeId).getResultList();
        Map<Integer, Integer> r = new HashMap<>();
        for (Object[] f : filas) {
            r.put(((Number) f[0]).intValue(), ((Number) f[1]).intValue());
        }
        return r;
    }


    private ViajeDeCompra aDominio(CompraLoteEntidad e) {
        List<GastoDeViaje> gastos = e.getGastos().stream().map(g -> new GastoDeViaje(g.getConcepto(), g.getMonto(), g.isCuentaParaCubrir())).toList();
        List<ProductoDelViaje> productos = e.getProductos().stream().map(f -> new ProductoDelViaje(f.getProducto().getId(), f.getProducto().getNombre(),
                f.getProducto().getCodigoBarras() == null ? null : f.getProducto().getCodigoBarras().getCodigoBarras(),
                f.getPiezasCompradas(), f.getCostoUnitario(), f.getFletePorPieza())).toList();
        return new ViajeDeCompra(e.getId(), e.getNombre(), e.getFechaCompra(), e.getFleteTotal(), e.getNota(), gastos, productos);
    }
}
