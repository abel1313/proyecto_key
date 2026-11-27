package com.ventas.key.hexagonal.whatsapp.infraestructura.salida.pedido;

import com.ventas.key.hexagonal.articulo.dominio.modelo.ArticuloAVender;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ArticuloParaPedido;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.CatalogoParaPedidoPort;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Lee el artículo de la base tal como está AHORA (precio, stock, habilitado, imagen). El precio es el que cobra la
 * tienda ({@code precioACobrar}: el normal, o el descuento solo si el dueño lo dejó activo en ese artículo).
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Gateway]</p>
 */
@Component
public class CatalogoParaPedidoJpaAdapter implements CatalogoParaPedidoPort {

    private final IVarianteRepository variantes;
    private final IVarianteImagenRepository imagenes;

    public CatalogoParaPedidoJpaAdapter(IVarianteRepository variantes, IVarianteImagenRepository imagenes) {
        this.variantes = variantes;
        this.imagenes = imagenes;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ArticuloParaPedido> articulo(int id) {
        return variantes.findById(id).map(v -> {
            Producto p = v.getProducto();
            boolean habilitado = p != null && p.getHabilitado() == '1' && v.getHabilitado() == '1';
            int stockProducto = p != null && p.getStock() != null ? p.getStock() : 0;
            Double precio = v.precioACobrar();
            return new ArticuloParaPedido(v.getId(),
                    ArticuloAVender.nombreVisible(p == null ? null : p.getNombre(), v.getTalla(), v.getColor()),
                    precio == null ? 0.0 : precio, Math.max(0, Math.min(v.getStock(), stockProducto)), habilitado,
                    p == null || Boolean.TRUE.equals(p.getEsCatalogoInterno()), imagenes.existsByVarianteId(v.getId()));
        });
    }
}
