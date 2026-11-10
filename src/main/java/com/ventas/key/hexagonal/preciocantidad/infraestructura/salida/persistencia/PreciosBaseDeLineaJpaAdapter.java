package com.ventas.key.hexagonal.preciocantidad.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.OrigenDePrecio;
import com.ventas.key.hexagonal.preciocantidad.dominio.puerto.salida.PreciosBaseDeLineaPort;
import com.ventas.key.mis.productos.entity.DetallePedidoPrecioGrupo;
import com.ventas.key.mis.productos.repository.IDetallePedidoPrecioGrupoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * [Hexagonal: Driven Adapter] [Clean: Interface Adapter] El precio de antes de las líneas cobradas con precio de grupo, en
 * {@code detalle_pedido_precio_grupo}. Sin la tabla (migración sin correr) no hace nada y no rompe la venta ni la edición.
 */
@Component
public class PreciosBaseDeLineaJpaAdapter implements PreciosBaseDeLineaPort {

    static final String TABLA = "detalle_pedido_precio_grupo";

    private final IDetallePedidoPrecioGrupoRepository repositorio;
    private final TablasDisponibles tablas;

    public PreciosBaseDeLineaJpaAdapter(IDetallePedidoPrecioGrupoRepository repositorio, TablasDisponibles tablas) {
        this.repositorio = repositorio;
        this.tablas = tablas;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, BigDecimal> leer(Collection<Integer> detalleIds, OrigenDePrecio origen) {
        Map<Integer, BigDecimal> r = new HashMap<>();
        if (detalleIds == null || detalleIds.isEmpty() || !tablas.existe(TABLA)) {
            return r;
        }
        repositorio.findByDetallePedidoIdIn(detalleIds).stream()
                .filter(f -> origen.name().equals(f.getOrigen()))
                .forEach(f -> r.put(f.getDetallePedidoId(), f.getPrecioSinGrupo()));
        return r;
    }

    @Override
    @Transactional
    public void guardar(Map<Integer, BigDecimal> preciosSinGrupo, OrigenDePrecio origen) {
        if (preciosSinGrupo == null || preciosSinGrupo.isEmpty() || !tablas.existe(TABLA)) {
            return;
        }
        preciosSinGrupo.forEach((detalleId, precio) -> {
            DetallePedidoPrecioGrupo fila = repositorio.findById(detalleId).orElseGet(DetallePedidoPrecioGrupo::new);
            fila.setDetallePedidoId(detalleId);
            fila.setPrecioSinGrupo(precio);
            fila.setOrigen(origen.name());
            repositorio.save(fila);
        });
    }

    @Override
    @Transactional
    public void borrar(Collection<Integer> detalleIds) {
        if (detalleIds == null || detalleIds.isEmpty() || !tablas.existe(TABLA)) {
            return;
        }
        repositorio.deleteAllById(repositorio.findByDetallePedidoIdIn(detalleIds).stream().map(DetallePedidoPrecioGrupo::getDetallePedidoId).toList());
    }
}
