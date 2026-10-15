package com.ventas.key.hexagonal.entrega.aplicacion.servicio;

import com.ventas.key.hexagonal.entrega.dominio.modelo.Entrega;
import com.ventas.key.hexagonal.entrega.dominio.puerto.entrada.EntregarPedidoCasoUso;
import com.ventas.key.hexagonal.entrega.dominio.puerto.salida.PedidosParaEntregarPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * [Hexagonal: Application Service] [Clean: Use Case Interactor]
 *
 * <p>Orquesta: lee el pedido (o su grupo), le pide a {@link Entrega} que decida y guarda.
 */
@Service
@RequiredArgsConstructor
public class EntregaService implements EntregarPedidoCasoUso {

    private final PedidosParaEntregarPort pedidos;

    @Override
    @Transactional
    public ResultadoEntrega entregar(Integer pedidoId) {
        List<Integer> ids = new Entrega(pedidoId, pedidos.delPedidoOSuGrupo(pedidoId)).entregar();
        pedidos.marcarEntrega(ids, true);
        return new ResultadoEntrega(ids, true);
    }

    @Override
    @Transactional
    public ResultadoEntrega regresar(Integer pedidoId) {
        List<Integer> ids = new Entrega(pedidoId, pedidos.delPedidoOSuGrupo(pedidoId)).regresar();
        pedidos.marcarEntrega(ids, false);
        return new ResultadoEntrega(ids, false);
    }
}
