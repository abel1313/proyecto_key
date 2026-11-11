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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** [Hexagonal: Driven Adapter] [Clean: Interface Adapter] Guarda y lee los viajes de compra en MySQL. */
@Component
public class ViajesJpaAdapter implements ViajesPort {

    private final ICompraLoteRepository repositorio;

    @PersistenceContext
    private EntityManager em;

    public ViajesJpaAdapter(ICompraLoteRepository repositorio) {
        this.repositorio = repositorio;
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
        e.getProductos().clear();
        if (e.getId() != null) {
            repositorio.saveAndFlush(e);   // borra las filas viejas antes de insertar las nuevas (productos únicos por viaje)
        }
        for (GastoDeViaje g : v.gastos()) {
            CompraLoteGastoEntidad fila = new CompraLoteGastoEntidad();
            fila.setLote(e);
            fila.setConcepto(g.concepto());
            fila.setMonto(g.monto());
            fila.setCuentaParaCubrir(g.cuentaParaCubrir());
            e.getGastos().add(fila);
        }
        for (ProductoDelViaje p : v.productos()) {
            Producto producto = em.find(Producto.class, p.productoId());
            if (producto == null) {
                throw new ViajeInvalidoException("El producto " + (p.nombre() == null ? p.productoId() : "«" + p.nombre() + "»") + " ya no existe.");
            }
            CompraLoteProductoEntidad fila = new CompraLoteProductoEntidad();
            fila.setLote(e);
            fila.setProducto(producto);
            fila.setPiezasCompradas(p.piezas());
            fila.setCostoUnitario(p.costoUnitario());
            fila.setFletePorPieza(p.fletePorPieza());
            e.getProductos().add(fila);
        }
        return aDominio(repositorio.saveAndFlush(e));
    }

    @Override
    @Transactional
    public void eliminar(int id) {
        repositorio.deleteById(id);
    }

    private ViajeDeCompra aDominio(CompraLoteEntidad e) {
        List<GastoDeViaje> gastos = e.getGastos().stream().map(g -> new GastoDeViaje(g.getConcepto(), g.getMonto(), g.isCuentaParaCubrir())).toList();
        List<ProductoDelViaje> productos = e.getProductos().stream().map(f -> new ProductoDelViaje(f.getProducto().getId(), f.getProducto().getNombre(),
                f.getProducto().getCodigoBarras() == null ? null : f.getProducto().getCodigoBarras().getCodigoBarras(),
                f.getPiezasCompradas(), f.getCostoUnitario(), f.getFletePorPieza())).toList();
        return new ViajeDeCompra(e.getId(), e.getNombre(), e.getFechaCompra(), e.getFleteTotal(), e.getNota(), gastos, productos);
    }
}
