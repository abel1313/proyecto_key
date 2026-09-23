package com.ventas.key.hexagonal.grupopedido.infraestructura.dto;

import com.ventas.key.hexagonal.grupopedido.dominio.modelo.GrupoPedidos;

import java.util.List;

/**
 * Lo que la card de la lista de pedidos necesita saber del grupo en el que esta el pedido.
 *
 * @param esTitular    si esta card es la del pedido titular (la unica que sale en la lista)
 * @param otrosPedidos los demas pedidos del grupo, sin este
 * @param saldoGrupo   lo que falta cobrar entre todos; en un grupo de contado es lo que cobra "Cobrar"
 */
public record GrupoEnListaResponse(
        Integer grupoId,
        Integer pedidoTitularId,
        boolean esTitular,
        String titularNombre,
        String tipoPedido,
        List<Integer> otrosPedidos,
        double totalGrupo,
        double pagadoGrupo,
        double saldoGrupo) {

    public static GrupoEnListaResponse de(GrupoPedidos g, Integer pedidoId) {
        GrupoPedidosResponse r = GrupoPedidosResponse.de(g);
        List<Integer> otros = r.pedidos().stream()
                .map(GrupoPedidosResponse.PedidoItem::pedidoId)
                .filter(id -> !id.equals(pedidoId))
                .toList();
        return new GrupoEnListaResponse(r.grupoId(), r.pedidoTitularId(), pedidoId.equals(r.pedidoTitularId()),
                r.titularNombre(), r.tipoPedido(), otros, r.totalGrupo(), r.pagadoGrupo(), r.saldoGrupo());
    }
}
