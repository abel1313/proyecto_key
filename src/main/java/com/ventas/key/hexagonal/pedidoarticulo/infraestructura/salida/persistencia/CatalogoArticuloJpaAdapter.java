package com.ventas.key.hexagonal.pedidoarticulo.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.ArticuloDisponible;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.PrecioCatalogo;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida.CatalogoArticuloPort;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Lee un articulo del catalogo con su stock y sus precios.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 */
@Component
@RequiredArgsConstructor
public class CatalogoArticuloJpaAdapter implements CatalogoArticuloPort {

    private final IVarianteRepository varianteRepository;
    private final IProductosRepository productoRepository;

    @Override
    public Optional<ArticuloDisponible> leerParaEditar(Integer varianteId) {
        Optional<Variantes> variante = varianteRepository.findById(varianteId);
        if (variante.isEmpty() || variante.get().getProducto() == null) {
            return Optional.empty();
        }
        Variantes v = variante.get();

        // findByIdWithLock y no findById: bloquea la fila del producto para el resto de la
        // transaccion, para que dos ediciones simultaneas del mismo modelo no lean las dos el
        // mismo stock y crean las dos que alcanza. Es como ya lo lee la creacion de pedidos.
        Producto producto = productoRepository.findByIdWithLock(v.getProducto().getId())
                .orElse(null);
        if (producto == null) {
            return Optional.empty();
        }

        return Optional.of(new ArticuloDisponible(
                v.getId(),
                producto.getId(),
                nombreDe(producto, v),
                v.getHabilitado() == '1',
                v.getStock(),
                producto.getStock() != null ? producto.getStock() : 0,
                new PrecioCatalogo(producto.getNombre(), producto.getPrecioVenta(), producto.getPrecioRebaja())));
    }

    private static String nombreDe(Producto producto, Variantes variante) {
        StringBuilder sb = new StringBuilder(producto.getNombre() != null ? producto.getNombre() : "Articulo");
        if (variante.getTalla() != null && !variante.getTalla().isBlank()) {
            sb.append(" talla ").append(variante.getTalla());
        }
        if (variante.getColor() != null && !variante.getColor().isBlank()) {
            sb.append(" ").append(variante.getColor());
        }
        return sb.toString();
    }
}
