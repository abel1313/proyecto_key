package com.ventas.key.hexagonal.reportecategoria.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.CalculoVigente;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.NuevoCalculo;
import com.ventas.key.hexagonal.reportecategoria.dominio.puerto.salida.CalculosVigentesPort;
import com.ventas.key.mis.productos.entity.ProductoCalculoPrecioEntidad;
import com.ventas.key.mis.productos.repository.IProductoCalculoPrecioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** [Hexagonal: Driven Adapter] Lee y guarda en {@code producto_calculo_precio}, el mismo historial que usa la calculadora de precio. */
@Component
@RequiredArgsConstructor
public class CalculosVigentesJpaAdapter implements CalculosVigentesPort {

    private static final int LOTE = 500;
    static final String NOTA_GENERADO = "Generado por el reporte por categoría · sin flete";

    private final IProductoCalculoPrecioRepository repo;

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, CalculoVigente> ultimos(Collection<Integer> productoIds) {
        Map<Integer, CalculoVigente> mapa = new HashMap<>();
        List<Integer> ids = new ArrayList<>(productoIds);
        for (int i = 0; i < ids.size(); i += LOTE) {
            for (ProductoCalculoPrecioEntidad c : repo.ultimosDe(ids.subList(i, Math.min(ids.size(), i + LOTE)))) {
                mapa.put(c.getProductoId(), new CalculoVigente(c.getProductoId(), c.getMargenPct(), c.getTransporte()));
            }
        }
        return mapa;
    }

    @Override
    @Transactional
    public void guardarGenerados(List<NuevoCalculo> nuevos, String usuario) {
        LocalDateTime ahora = LocalDateTime.now();
        List<ProductoCalculoPrecioEntidad> filas = new ArrayList<>();
        for (NuevoCalculo n : nuevos) {
            ProductoCalculoPrecioEntidad e = new ProductoCalculoPrecioEntidad();
            e.setProductoId(n.productoId());
            e.setCostoCompra(n.costo());
            e.setTransporte(BigDecimal.ZERO);
            e.setOtros(BigDecimal.ZERO);
            e.setComisionPct(BigDecimal.ZERO);
            e.setMargenPct(n.margenPct());
            e.setPrecioCalculado(n.precioVenta());
            e.setPrecioCobrado(n.precioVenta());
            e.setActualizoPrecioCompra(false);
            e.setNota(NOTA_GENERADO);
            e.setUsuario(usuario);
            e.setFecha(ahora);
            filas.add(e);
        }
        repo.saveAll(filas);
    }
}
