package com.ventas.key.hexagonal.entrega.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.entrega.dominio.modelo.PedidoParaEntregar;
import com.ventas.key.hexagonal.entrega.dominio.puerto.salida.PedidosParaEntregarPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks &amp; Drivers]
 *
 * <p>SQL directo sobre {@code pedidos} y el grupo activo ({@code grupo_pedido} +
 * {@code grupo_pedido_miembro}). Columnas de migration_entrega_pedido.sql.
 */
@Component
@RequiredArgsConstructor
public class PedidosParaEntregarJdbcAdapter implements PedidosParaEntregarPort {

    private static final String PEDIDOS = """
            SELECT p.id, p.tipo_pedido, p.estado_pedido, p.total_pedido, p.total_pagado, p.entregado
            FROM pedidos p
            WHERE p.id IN (
                SELECT :id
                UNION
                SELECT gm2.pedido_id
                FROM grupo_pedido_miembro gm
                JOIN grupo_pedido g ON g.id = gm.grupo_id AND g.activo = 1
                JOIN grupo_pedido_miembro gm2 ON gm2.grupo_id = g.id
                WHERE gm.pedido_id = :id)
            ORDER BY p.id""";

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<PedidoParaEntregar> delPedidoOSuGrupo(Integer pedidoId) {
        return jdbc.query(PEDIDOS, new MapSqlParameterSource("id", pedidoId), (rs, i) -> new PedidoParaEntregar(
                rs.getInt("id"),
                rs.getString("tipo_pedido"),
                rs.getString("estado_pedido"),
                Math.round(rs.getDouble("total_pedido") * 100),
                Math.round(rs.getDouble("total_pagado") * 100),
                rs.getBoolean("entregado")));
    }

    @Override
    public void marcarEntrega(List<Integer> pedidoIds, boolean entregado) {
        if (pedidoIds.isEmpty()) {
            return;
        }
        jdbc.update("UPDATE pedidos SET entregado = :entregado, fecha_entregado = "
                        + (entregado ? "NOW()" : "NULL") + " WHERE id IN (:ids)",
                new MapSqlParameterSource("entregado", entregado).addValue("ids", pedidoIds));
    }
}
