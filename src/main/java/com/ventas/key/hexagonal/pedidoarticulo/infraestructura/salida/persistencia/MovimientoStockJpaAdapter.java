package com.ventas.key.hexagonal.pedidoarticulo.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida.MovimientoStockPort;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mueve el stock del articulo y de su modelo al editar un pedido.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 *
 * <p>Toca las dos tablas porque el sistema lleva la cuenta en las dos -- un detalle del modelo de
 * datos que el dominio no tiene por que conocer, y por eso vive aqui y no alla.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MovimientoStockJpaAdapter implements MovimientoStockPort {

    private final IVarianteRepository varianteRepository;
    private final IProductosRepository productoRepository;

    @Override
    public void descontar(Integer varianteId, int cantidad) {
        mover(varianteId, -cantidad);
    }

    @Override
    public void devolver(Integer varianteId, int cantidad) {
        mover(varianteId, +cantidad);
    }

    private void mover(Integer varianteId, int delta) {
        Variantes variante = varianteRepository.findById(varianteId)
                .orElseThrow(() -> new IllegalStateException("Articulo no encontrado al mover stock: " + varianteId));

        variante.setStock(noNegativo(variante.getStock() + delta, "articulo " + varianteId));
        varianteRepository.save(variante);

        if (variante.getProducto() != null) {
            Producto producto = productoRepository.findByIdWithLock(variante.getProducto().getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Modelo no encontrado al mover stock: " + variante.getProducto().getId()));
            int actual = producto.getStock() != null ? producto.getStock() : 0;
            producto.setStock(noNegativo(actual + delta, "modelo " + producto.getId()));
            productoRepository.save(producto);
        }
    }

    /**
     * Red de seguridad: el stock nunca se guarda negativo.
     *
     * <p>El servicio ya valida que alcance antes de descontar, asi que llegar aqui en negativo
     * significa que el inventario ya venia descuadrado de antes (paso en produccion el
     * 2026-09-22). Se corta en 0 y se deja el rastro en el log en vez de propagar el descuadre.
     */
    private static int noNegativo(int valor, String quien) {
        if (valor < 0) {
            log.warn("El stock del {} habria quedado en {} al editar un pedido; se deja en 0. "
                    + "Esto significa que ya venia descuadrado", quien, valor);
            return 0;
        }
        return valor;
    }
}
