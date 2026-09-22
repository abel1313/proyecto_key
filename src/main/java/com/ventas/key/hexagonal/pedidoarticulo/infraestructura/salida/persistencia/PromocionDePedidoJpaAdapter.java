package com.ventas.key.hexagonal.pedidoarticulo.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.PromocionDelPedido;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida.PromocionDePedidoPort;
import com.ventas.key.mis.productos.entity.Promocion;
import com.ventas.key.mis.productos.entity.PromocionDetalle;
import com.ventas.key.mis.productos.repository.IPromocionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Lee el combo de una promocion: que articulos la componen y a que precio.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 */
@Component
@RequiredArgsConstructor
public class PromocionDePedidoJpaAdapter implements PromocionDePedidoPort {

    private final IPromocionRepository promocionRepository;

    @Override
    public Optional<PromocionDelPedido> buscar(Integer promocionId) {
        return promocionRepository.findByIdConDetalle(promocionId).map(PromocionDePedidoJpaAdapter::aDominio);
    }

    private static PromocionDelPedido aDominio(Promocion promo) {
        Map<Integer, Double> precios = new LinkedHashMap<>();
        if (promo.getDetalles() != null) {
            for (PromocionDetalle d : promo.getDetalles()) {
                if (d.getVariante() != null) {
                    precios.put(d.getVariante().getId(), d.getPrecioEnPromocion());
                }
            }
        }

        // La vigencia se informa, no se exige: un pedido que ya tiene la promocion aplicada
        // conserva su precio aunque la promocion venza (R2 del dominio promocion). Lo que se
        // necesita aqui es saber que articulos la componen, y eso no caduca.
        boolean vigente = Boolean.TRUE.equals(promo.getActivo())
                && promo.getFechaVencimiento() != null
                && promo.getFechaVencimiento().isAfter(LocalDateTime.now());

        return new PromocionDelPedido(promo.getId(), promo.getDescripcion(), vigente, precios);
    }
}
