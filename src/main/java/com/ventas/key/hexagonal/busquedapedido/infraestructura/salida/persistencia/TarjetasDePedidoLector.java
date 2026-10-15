package com.ventas.key.hexagonal.busquedapedido.infraestructura.salida.persistencia;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.GrupoPedidos;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.entrada.ConsultarGruposCasoUso;
import com.ventas.key.hexagonal.grupopedido.infraestructura.dto.GrupoEnListaResponse;
import com.ventas.key.mis.productos.models.pedidos.PedidoGenerico;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * [Hexagonal: Driven Adapter] [Clean: Interface Adapters — Presenter]
 *
 * <p>Arma la card de Mis pedidos de los pedidos de una pagina, con <b>el mismo JSON</b> que
 * {@code GET /v1/pedidos/buscarClientePedido} (mismo SELECT que {@code IPedidoRepository
 * .buscarTodosLosPedidos}), para que la card del front no cambie. Ademas le pone a cada uno su
 * grupo activo, igual que {@code PedidoServiceImpl.marcarGrupos}.
 *
 * <p>Separado de la busqueda a proposito: filtrar decide <i>cuales</i> pedidos y en que orden;
 * esto solo los dibuja.
 */
@Component
@RequiredArgsConstructor
public class TarjetasDePedidoLector {

    private static final String TARJETAS = """
            SELECT p.id AS pedido_id,
              JSON_OBJECT(
                'cliente', JSON_OBJECT(
                  'id',                COALESCE(csr.id, c.id),
                  'nombreCliente',     COALESCE(csr.nombre_persona, NULLIF(TRIM(p.nombre_receptor), ''), c.nombre_persona),
                  'correoElectronico', COALESCE(csr.correo_electronico, c.correo_electronico),
                  'numeroTelefonico',  COALESCE(csr.numero_telefonico, c.numero_telefonico),
                  'sinRegistro',       csr.id IS NOT NULL
                ),
                'pedido', JSON_OBJECT(
                  'id', p.id,
                  'fecha_pedido', DATE_FORMAT(COALESCE(p.fecha_hora_registro, p.fecha_pedido), '%d/%m/%Y %H:%i'),
                  'estado_pedido', p.estado_pedido,
                  'tipoPedido', p.tipo_pedido,
                  'totalPagado', p.total_pagado,

                  'entregado', IF(p.entregado = 1, CAST('true' AS JSON), CAST('false' AS JSON)),
                  'nombreReceptor', p.nombre_receptor,
                  'lugarEntregaId', le.id,
                  'lugarEntregaNombre', le.nombre,
                  'urlFacebook', p.url_facebook,
                  'fechaEntrega', DATE_FORMAT(p.fecha_recogida, '%Y-%m-%d'),
                  'horaEntrega', p.hora_recogida,
                  'recogeEnLocal', IF(le.id IS NULL OR le.es_recoger_en_tienda = 1, CAST('true' AS JSON), CAST('false' AS JSON)),
                  'detalles', JSON_ARRAYAGG(
                    JSON_OBJECT(
                      'nombre_producto', pro.nombre,
                      'producto', dp.producto_id,
                      'cantidad', dp.cantidad,
                      'precio_unitario', dp.precio_unitario,
                      'sub_total', dp.sub_total
                    )
                  )
                )
              ) AS pedido_json
            FROM pedidos p
            INNER JOIN detalle_pedidos dp ON p.id = dp.pedido_id
            LEFT  JOIN clientes c               ON c.id   = p.cliente_id
            LEFT  JOIN clientes_sin_registro csr ON csr.id = p.cliente_sin_registro_id
            LEFT  JOIN lugares_entrega le       ON le.id  = p.lugar_entrega_id
            INNER JOIN producto pro ON pro.id = dp.producto_id
            WHERE p.id IN (:ids)
            GROUP BY p.id, p.fecha_pedido, p.estado_pedido, c.id, csr.id, le.id, le.nombre
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final ConsultarGruposCasoUso consultarGrupos;

    /** Las cards en el mismo orden que {@code pedidoIds} (el SQL con IN no respeta el orden). */
    public List<PedidoGenerico> tarjetas(List<Integer> pedidoIds) {
        if (pedidoIds.isEmpty()) {
            return List.of();
        }
        Map<Integer, String> jsonPorId = new HashMap<>();
        jdbc.query(TARJETAS, new MapSqlParameterSource("ids", pedidoIds),
                rs -> {
                    jsonPorId.put(rs.getInt("pedido_id"), rs.getString("pedido_json"));
                });

        List<PedidoGenerico> tarjetas = pedidoIds.stream()
                .map(jsonPorId::get)
                .filter(Objects::nonNull)
                .map(this::leer)
                .toList();
        marcarGrupos(tarjetas);
        return tarjetas;
    }

    private PedidoGenerico leer(String json) {
        try {
            return objectMapper.readValue(json, PedidoGenerico.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo leer la card del pedido", e);
        }
    }

    private void marcarGrupos(List<PedidoGenerico> tarjetas) {
        if (tarjetas.isEmpty()) {
            return;
        }
        Map<Integer, GrupoPedidos> grupos = consultarGrupos.gruposActivosDe(
                tarjetas.stream().map(t -> t.getPedido().getId()).toList());
        Map<Integer, Boolean> entregados = entregadosDe(grupos.values().stream()
                .flatMap(g -> g.pedidos().stream()).filter(p -> !p.estaCancelado())
                .map(p -> p.pedidoId()).distinct().toList());
        for (PedidoGenerico t : tarjetas) {
            GrupoPedidos grupo = grupos.get(t.getPedido().getId());
            if (grupo != null) {
                // E7 (dominio entrega): la card del titular dice Entregado solo si todos los del
                // grupo, sin los cancelados, ya se lo llevaron.
                boolean todos = grupo.pedidos().stream().filter(p -> !p.estaCancelado())
                        .allMatch(p -> Boolean.TRUE.equals(entregados.get(p.pedidoId())));
                t.getPedido().setGrupo(GrupoEnListaResponse.de(grupo, t.getPedido().getId(), todos));
            }
        }
    }

    private Map<Integer, Boolean> entregadosDe(List<Integer> pedidoIds) {
        Map<Integer, Boolean> entregados = new HashMap<>();
        if (pedidoIds.isEmpty()) {
            return entregados;
        }
        jdbc.query("SELECT id, entregado FROM pedidos WHERE id IN (:ids)",
                new MapSqlParameterSource("ids", pedidoIds),
                rs -> { entregados.put(rs.getInt("id"), rs.getBoolean("entregado")); });
        return entregados;
    }
}
