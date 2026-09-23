package com.ventas.key.hexagonal.grupopedido.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.BitacoraPedidoPort;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Agrega una linea a {@code pedidos.observaciones}, sin borrar lo que ya habia.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 */
@Component
@RequiredArgsConstructor
public class BitacoraPedidoJpaAdapter implements BitacoraPedidoPort {

    private final IPedidoRepository pedidoRepository;

    @Override
    public void anotar(Integer pedidoId, String texto) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalStateException("Pedido no encontrado: " + pedidoId));
        String previas = pedido.getObservaciones();
        pedido.setObservaciones(previas == null || previas.isBlank() ? texto : previas + "\n" + texto);
        pedidoRepository.save(pedido);
    }
}
