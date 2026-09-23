package com.ventas.key.hexagonal.precio.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.precio.dominio.modelo.PreciosDeProducto;
import com.ventas.key.hexagonal.precio.dominio.puerto.salida.PreciosProductoPort;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** [Hexagonal: Driven Adapter] [Clean: Frameworks &amp; Drivers] */
@Repository
@RequiredArgsConstructor
public class PreciosProductoJpaAdapter implements PreciosProductoPort {

    private final IProductosRepository productos;

    @Override
    public Optional<PreciosDeProducto> buscar(Integer productoId) {
        return productos.findById(productoId).map(p -> new PreciosDeProducto(
                p.getId(), p.getNombre(),
                valor(p.getPrecioCosto()), valor(p.getPrecioVenta()), valor(p.getPrecioRebaja())));
    }

    /** Solo los dos precios: el resto del producto no se toca desde aqui. */
    @Override
    public void guardar(PreciosDeProducto precios) {
        Producto p = productos.findById(precios.productoId()).orElseThrow();
        p.setPrecioVenta(precios.precioVenta());
        p.setPrecioRebaja(precios.precioRebaja());
        productos.save(p);
    }

    private static double valor(Double d) {
        return d == null ? 0.0 : d;
    }
}
