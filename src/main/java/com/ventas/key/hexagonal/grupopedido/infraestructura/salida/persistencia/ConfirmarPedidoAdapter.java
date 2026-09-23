package com.ventas.key.hexagonal.grupopedido.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.ConfirmarPedidoPort;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.models.pedidos.ClienteQuery;
import com.ventas.key.mis.productos.models.pedidos.PedidoGenerico;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import com.ventas.key.mis.productos.service.api.IPedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Confirma cada pedido con {@code PedidoServiceImpl.updatePedido}, lo mismo que
 * {@code PUT /v1/pedidos/confirmar/{id}}.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 */
@Component
@RequiredArgsConstructor
public class ConfirmarPedidoAdapter implements ConfirmarPedidoPort {

    private final IPedidoService pedidoService;
    private final IPedidoRepository pedidoRepository;

    @Override
    public void confirmarDeContado(Integer pedidoId, Integer pagosYMesesId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalStateException("Pedido no encontrado: " + pedidoId));

        // updatePedido busca al usuario que registra la venta por el cliente con cuenta del
        // pedido (en una venta de mostrador es la cuenta de quien atendio). Se toma del pedido y
        // no de la card: la card muestra primero al cliente sin registro, que no tiene usuario.
        ClienteQuery cliente = new ClienteQuery();
        cliente.setId(pedido.getCliente() != null ? pedido.getCliente().getId() : 0);

        PedidoGenerico request = new PedidoGenerico();
        request.setCliente(cliente);
        request.setPagosYMesesId(pagosYMesesId);
        try {
            pedidoService.updatePedido(pedidoId, request);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Pedido #" + pedidoId + ": " + e.getMessage(), e);
        }
    }
}
