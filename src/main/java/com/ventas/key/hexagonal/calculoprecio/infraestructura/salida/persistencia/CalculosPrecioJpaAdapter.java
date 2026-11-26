package com.ventas.key.hexagonal.calculoprecio.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.calculoprecio.dominio.excepcion.CalculoPrecioInvalidoException;
import com.ventas.key.hexagonal.calculoprecio.dominio.modelo.CalculoDeProducto;
import com.ventas.key.hexagonal.calculoprecio.dominio.modelo.CalculoPrecio;
import com.ventas.key.hexagonal.calculoprecio.dominio.modelo.FiltroCalculos;
import com.ventas.key.hexagonal.calculoprecio.dominio.modelo.PaginaCalculos;
import com.ventas.key.hexagonal.calculoprecio.dominio.puerto.salida.CalculosPrecioPort;
import com.ventas.key.mis.productos.entity.ProductoCalculoPrecioEntidad;
import com.ventas.key.mis.productos.repository.IProductoCalculoPrecioRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** [Hexagonal: Driven Adapter] [Clean: Interface Adapter] Guarda y lee los cálculos de precio en MySQL. */
@Slf4j
@Component
public class CalculosPrecioJpaAdapter implements CalculosPrecioPort {

    private final IProductoCalculoPrecioRepository repositorio;
    private final IProductosRepository productos;

    public CalculosPrecioJpaAdapter(IProductoCalculoPrecioRepository repositorio, IProductosRepository productos) {
        this.repositorio = repositorio;
        this.productos = productos;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeProducto(int productoId) {
        return productos.existsById(productoId);
    }

    @Override
    @Transactional
    public CalculoPrecio guardar(CalculoPrecio c) {
        ProductoCalculoPrecioEntidad e = new ProductoCalculoPrecioEntidad();
        e.setProductoId(c.productoId());
        e.setCostoCompra(c.costoCompra());
        e.setTransporte(c.transporte());
        e.setOtros(c.otros());
        e.setComisionPct(c.comisionPct());
        e.setComisionDescripcion(c.comisionDescripcion());
        e.setMargenPct(c.margenPct());
        e.setPrecioCalculado(c.precioCalculado());
        e.setPrecioCobrado(c.precioCobrado());
        e.setActualizoPrecioCompra(c.actualizoPrecioCompra());
        e.setNota(c.nota());
        e.setUsuario(c.usuario());
        e.setFecha(c.fecha());
        return aDominio(repositorio.save(e));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CalculoPrecio> ultimo(int productoId) {
        return repositorio.findFirstByProductoIdOrderByFechaDescIdDesc(productoId).flatMap(this::aDominioOmitiendoInvalido);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalculoPrecio> historial(int productoId, int maximo) {
        return repositorio.findByProductoIdOrderByFechaDescIdDesc(productoId, PageRequest.of(0, maximo)).stream().map(this::aDominioOmitiendoInvalido).flatMap(Optional::stream).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaCalculos ultimosPorProducto(FiltroCalculos f, int pagina, int tamano) {
        LocalDateTime desde = (f.desde() != null ? f.desde() : LocalDate.of(2000, 1, 1)).atStartOfDay();
        LocalDateTime hasta = (f.hasta() != null ? f.hasta() : LocalDate.of(2999, 12, 31)).plusDays(1).atStartOfDay();
        Page<Object[]> p = repositorio.ultimosPorProducto(f.nombre(), desde, hasta, PageRequest.of(pagina - 1, tamano));
        List<CalculoDeProducto> items = p.getContent().stream().map(fila -> aDominioOmitiendoInvalido((ProductoCalculoPrecioEntidad) fila[0]).map(c -> new CalculoDeProducto(
                c, (String) fila[1],
                fila[2] == null ? null : BigDecimal.valueOf(((Number) fila[2]).doubleValue()).setScale(2, java.math.RoundingMode.HALF_UP))))
                .flatMap(Optional::stream).toList();
        return new PaginaCalculos(items, p.getTotalElements(), pagina, tamano);
    }

    /** Una fila vieja que ya no cumple las reglas (p. ej. margen negativo) no tumba la lista: se omite y se avisa en el log con su id. */
    private Optional<CalculoPrecio> aDominioOmitiendoInvalido(ProductoCalculoPrecioEntidad e) {
        try {
            return Optional.of(aDominio(e));
        } catch (CalculoPrecioInvalidoException ex) {
            log.warn("Cálculo de precio {} (producto {}) omitido por datos fuera de las reglas: {}", e.getId(), e.getProductoId(), ex.getMessage());
            return Optional.empty();
        }
    }

    private CalculoPrecio aDominio(ProductoCalculoPrecioEntidad e) {
        return new CalculoPrecio(e.getId(), e.getProductoId(), e.getCostoCompra(), e.getTransporte(), e.getOtros(), e.getComisionPct(),
                e.getComisionDescripcion(), e.getMargenPct(), e.getPrecioCalculado(), e.getPrecioCobrado(), e.isActualizoPrecioCompra(),
                e.getNota(), e.getUsuario(), e.getFecha());
    }
}
