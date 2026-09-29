package com.ventas.key.hexagonal.precio.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.precio.dominio.modelo.PreciosDeArticulo;
import com.ventas.key.hexagonal.precio.dominio.puerto.salida.PreciosArticuloPort;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** [Hexagonal: Driven Adapter] [Clean: Frameworks &amp; Drivers] */
@Repository
@RequiredArgsConstructor
public class PreciosArticuloJpaAdapter implements PreciosArticuloPort {

    private final IVarianteRepository variantes;

    @Override
    public Optional<PreciosDeArticulo> buscar(Integer varianteId) {
        return variantes.findById(varianteId)
                .filter(v -> v.getProducto() != null)
                .map(v -> new PreciosDeArticulo(
                        v.getId(), nombreDe(v),
                        valor(v.getProducto().getPrecioCosto()),
                        valor(v.precioNormal()), valor(v.precioDescuento()),
                        v.tienePrecioPropio()));
    }

    /** Solo los dos precios del articulo: ni el producto ni el resto del articulo se tocan. */
    @Override
    public void guardar(PreciosDeArticulo precios) {
        Variantes v = variantes.findById(precios.varianteId()).orElseThrow();
        v.setPrecioVenta(precios.propio() ? precios.precioVenta() : null);
        v.setPrecioRebaja(precios.propio() ? precios.precioRebaja() : null);
        variantes.save(v);
    }

    private static String nombreDe(Variantes v) {
        Producto p = v.getProducto();
        StringBuilder sb = new StringBuilder(p.getNombre() != null ? p.getNombre() : "Artículo");
        if (v.getTalla() != null && !v.getTalla().isBlank()) {
            sb.append(" talla ").append(v.getTalla());
        }
        if (v.getColor() != null && !v.getColor().isBlank()) {
            sb.append(" ").append(v.getColor());
        }
        return sb.toString();
    }

    private static double valor(Double d) {
        return d == null ? 0.0 : d;
    }
}
