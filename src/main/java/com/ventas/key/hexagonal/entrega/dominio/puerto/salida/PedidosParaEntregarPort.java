package com.ventas.key.hexagonal.entrega.dominio.puerto.salida;

import com.ventas.key.hexagonal.entrega.dominio.modelo.PedidoParaEntregar;

import java.util.List;

/**
 * [Hexagonal: Driven Port] [Clean: Interface Adapter boundary]
 */
public interface PedidosParaEntregarPort {

    /** El pedido solo, o todos los de su grupo activo (el titular incluido). Vacio si no existe. */
    List<PedidoParaEntregar> delPedidoOSuGrupo(Integer pedidoId);

    /** Marca la entrega de esos pedidos, con la fecha de hoy (o sin fecha al regresarla). */
    void marcarEntrega(List<Integer> pedidoIds, boolean entregado);
}
