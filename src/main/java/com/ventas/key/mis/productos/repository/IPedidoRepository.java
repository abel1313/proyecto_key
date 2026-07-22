package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IPedidoRepository extends BaseRepository<Pedido,Integer>{

    List<Pedido> findByEstadoPedidoAndFechaRecogidaIsNotNullAndFechaRecogidaLessThanEqual(
            String estadoPedido, LocalDate fecha);

    // APARTADO = el producto NO se entrega hasta pagarse completo (a diferencia de FIADO,
    // que ya se entregó y solo queda pendiente el cobro) — por eso "pendiente de entregar"
    // solo cuenta APARTADO activos, no FIADO.
    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.tipoPedido = 'APARTADO' AND p.estadoPedido = 'APARTADO'")
    long countPendientesEntregar();

    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.estadoPedido IN ('APARTADO', 'FIADO')")
    long countCreditosActivos();

    @Query("SELECT COALESCE(SUM(p.totalPedido - p.totalPagado), 0) FROM Pedido p " +
           "WHERE p.estadoPedido IN ('APARTADO', 'FIADO')")
    Double sumMontoPorCobrar();

    @Query(value = """
        SELECT
            COALESCE(SUM(CASE WHEN p.estado_pedido = 'Entregado' THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN p.motivo_cancelacion IN ('TIMEOUT', 'NO_SE_PRESENTO') THEN 1 ELSE 0 END), 0),
            COALESCE((
                SELECT SUM(dp.cantidad)
                FROM detalle_pedidos dp
                INNER JOIN pedidos p2 ON p2.id = dp.pedido_id
                WHERE (
                    -- Pedido normal entregado en ese mes
                    (p2.tipo_pedido NOT IN ('APARTADO','FIADO') AND p2.estado_pedido = 'Entregado'
                     AND DATE_FORMAT(p2.fecha_pedido, '%Y-%m') = :mes)
                    OR
                    -- Crédito: contar en el mes que se liquidó
                    (p2.tipo_pedido IN ('APARTADO','FIADO') AND p2.estado_pedido = 'PAGADO'
                     AND :mes = (SELECT DATE_FORMAT(MAX(ap.fecha_pago), '%Y-%m')
                                 FROM abono_pedido ap WHERE ap.pedido_id = p2.id))
                )
                AND ((:sinRegistro = FALSE AND p2.cliente_id = :clienteId)
                  OR (:sinRegistro = TRUE  AND p2.cliente_sin_registro_id = :clienteId))
            ), 0)
        FROM pedidos p
        WHERE (:sinRegistro = FALSE AND p.cliente_id = :clienteId)
           OR (:sinRegistro = TRUE  AND p.cliente_sin_registro_id = :clienteId)
    """, nativeQuery = true)
    List<Object[]> calcularScore(@Param("clienteId") Integer clienteId,
                                @Param("sinRegistro") boolean sinRegistro,
                                @Param("mes") String mes);



    @Query(value = """
    SELECT 
      JSON_OBJECT(
        'cliente', JSON_OBJECT(
        'id', c.id,
        'nombreCliente', c.nombre_persona,
        'correoElectronico', c.correo_electronico,
        'numeroTelefonico', c.numero_telefonico\s
        ),
        'pedido', JSON_OBJECT(
          'id', p.id,
          'fecha_pedido', DATE_FORMAT(COALESCE(p.fecha_hora_registro, p.fecha_pedido), '%d/%m/%Y %H:%i'),
          'estado_pedido', p.estado_pedido,
          'tipoPedido', p.tipo_pedido,
          'totalPagado', p.total_pagado,
          'detalles', JSON_ARRAYAGG(
            JSON_OBJECT(
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
    INNER JOIN clientes c ON c.id = p.cliente_id
    WHERE c.id = :id
    GROUP BY p.id, p.fecha_pedido, p.estado_pedido, c.id
    ORDER BY p.fecha_pedido DESC
    """, nativeQuery = true)
    List<String> findPedidoPorId(@Param("id") int id, Pageable pegable);



    @Query(value = """
    SELECT 
      JSON_OBJECT(
        'cliente', JSON_OBJECT(
        'id', c.id,
        'nombreCliente', c.nombre_persona,
        'correoElectronico', c.correo_electronico,
        'numeroTelefonico', c.numero_telefonico\s
        ),
        'pedido', JSON_OBJECT(
          'id', p.id,
          'fecha_pedido', DATE_FORMAT(COALESCE(p.fecha_hora_registro, p.fecha_pedido), '%d/%m/%Y %H:%i'),
          'estado_pedido', p.estado_pedido,
          'tipoPedido', p.tipo_pedido,
          'totalPagado', p.total_pagado,
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
    INNER JOIN clientes c ON c.id = p.cliente_id
                INNER JOIN producto pro
        on pro.id = dp.producto_id 
    WHERE c.id = :id
    GROUP BY p.id, p.fecha_pedido, p.estado_pedido, c.id
    ORDER BY p.fecha_pedido DESC
    """, nativeQuery = true)
    Page<String> findPedidoPorId2(@Param("id") int id, Pageable pegable);


    @Query(value = """
    SELECT 
      JSON_OBJECT(
        'cliente', JSON_OBJECT(
        'id', c.id,
        'nombreCliente', c.nombre_persona,
        'correoElectronico', c.correo_electronico,
        'numeroTelefonico', c.numero_telefonico\s
        ),
        'pedido', JSON_OBJECT(
          'id', p.id,
          'fecha_pedido', DATE_FORMAT(COALESCE(p.fecha_hora_registro, p.fecha_pedido), '%d/%m/%Y %H:%i'),
          'estado_pedido', p.estado_pedido,
          'tipoPedido', p.tipo_pedido,
          'totalPagado', p.total_pagado,
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
    INNER JOIN clientes c ON c.id = p.cliente_id
        INNER JOIN producto pro
        on pro.id = dp.producto_id 
    WHERE c.id = :idCliente and p.id = :idPedido
    GROUP BY p.id, p.fecha_pedido, p.estado_pedido, c.id
    ORDER BY p.fecha_pedido DESC
    """, nativeQuery = true)
    Page<String> pediodPorId(@Param("idPedido") int idPedido,@Param("idCliente") int idCliente, Pageable pegable);

    @Query(value = """
    SELECT
      JSON_OBJECT(
        'cliente', JSON_OBJECT(
          'id',                COALESCE(c.id, csr.id),
          'nombreCliente',     COALESCE(c.nombre_persona, csr.nombre_persona),
          'correoElectronico', COALESCE(c.correo_electronico, csr.correo_electronico),
          'numeroTelefonico',  COALESCE(c.numero_telefonico, csr.numero_telefonico),
          'sinRegistro',       c.id IS NULL
        ),
        'pedido', JSON_OBJECT(
          'id', p.id,
          'fecha_pedido', DATE_FORMAT(COALESCE(p.fecha_hora_registro, p.fecha_pedido), '%d/%m/%Y %H:%i'),
          'estado_pedido', p.estado_pedido,
          'tipoPedido', p.tipo_pedido,
          'totalPagado', p.total_pagado,
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
    LEFT  JOIN clientes c              ON c.id   = p.cliente_id
    LEFT  JOIN clientes_sin_registro csr ON csr.id = p.cliente_sin_registro_id
    INNER JOIN producto pro ON pro.id = dp.producto_id
    WHERE c.nombre_persona     LIKE CONCAT('%', :buscar, '%')
       OR c.correo_electronico LIKE CONCAT('%', :buscar, '%')
       OR c.numero_telefonico  LIKE CONCAT('%', :buscar, '%')
       OR csr.nombre_persona   LIKE CONCAT('%', :buscar, '%')
       OR csr.numero_telefonico LIKE CONCAT('%', :buscar, '%')
    GROUP BY p.id, p.fecha_pedido, p.estado_pedido, c.id, csr.id
    ORDER BY p.fecha_pedido DESC
    """, nativeQuery = true)
    Page<String> buscarPedidosPorCliente(@Param("buscar") String buscar, Pageable pegable);


    // Elegibilidad de rifa por mes: cualquiera que haya comprado ese mes entra,
    // sin importar si tiene correo o telefono registrado.
    @Query(value = """
        SELECT DISTINCT
            COALESCE(c.id, csr.id)                                           AS clientePedidoId,
            COALESCE(c.nombre_persona, csr.nombre_persona)                   AS nombre,
            COALESCE(c.numero_telefonico, csr.numero_telefonico)             AS telefono,
            COALESCE(c.correo_electronico, csr.correo_electronico)           AS correo,
            c.id IS NULL                                                     AS sinRegistro
        FROM pedidos p
        LEFT  JOIN clientes c              ON c.id   = p.cliente_id
        LEFT  JOIN clientes_sin_registro csr ON csr.id = p.cliente_sin_registro_id
        WHERE (
            -- Pedido normal: usar fecha del pedido
            (p.tipo_pedido NOT IN ('APARTADO','FIADO') AND DATE_FORMAT(p.fecha_pedido, '%Y-%m') = :mes)
            OR
            -- Crédito: contar en el mes en que se liquidó (último abono)
            (p.tipo_pedido IN ('APARTADO','FIADO') AND p.estado_pedido = 'PAGADO'
             AND :mes = (SELECT DATE_FORMAT(MAX(ap.fecha_pago), '%Y-%m')
                         FROM abono_pedido ap WHERE ap.pedido_id = p.id))
        )
        ORDER BY nombre
    """, nativeQuery = true)
    List<Object[]> findClientesUnicosPorMes(@Param("mes") String mes);

    @Query(value = """
        SELECT DISTINCT
            COALESCE(c.id, csr.id)                                           AS clientePedidoId,
            COALESCE(c.nombre_persona, csr.nombre_persona)                   AS nombre,
            COALESCE(c.numero_telefonico, csr.numero_telefonico)             AS telefono,
            COALESCE(c.correo_electronico, csr.correo_electronico)           AS correo,
            c.id IS NULL                                                     AS sinRegistro
        FROM pedidos p
        LEFT  JOIN clientes c              ON c.id   = p.cliente_id
        LEFT  JOIN clientes_sin_registro csr ON csr.id = p.cliente_sin_registro_id
        WHERE (p.cliente_id IS NOT NULL OR p.cliente_sin_registro_id IS NOT NULL)
          AND (p.tipo_pedido NOT IN ('APARTADO','FIADO') OR p.estado_pedido = 'PAGADO')
        ORDER BY nombre
    """, nativeQuery = true)
    List<Object[]> findTodosClientesConCompras();


    @Query(value = """
    SELECT
      JSON_OBJECT(
        'cliente', JSON_OBJECT(
          'id',                COALESCE(c.id, csr.id),
          'nombreCliente',     COALESCE(c.nombre_persona, csr.nombre_persona),
          'correoElectronico', COALESCE(c.correo_electronico, csr.correo_electronico),
          'numeroTelefonico',  COALESCE(c.numero_telefonico, csr.numero_telefonico),
          'sinRegistro',       c.id IS NULL
        ),
        'pedido', JSON_OBJECT(
          'id', p.id,
          'fecha_pedido', DATE_FORMAT(COALESCE(p.fecha_hora_registro, p.fecha_pedido), '%d/%m/%Y %H:%i'),
          'estado_pedido', p.estado_pedido,
          'tipoPedido', p.tipo_pedido,
          'totalPagado', p.total_pagado,
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
    LEFT  JOIN clientes c              ON c.id   = p.cliente_id
    LEFT  JOIN clientes_sin_registro csr ON csr.id = p.cliente_sin_registro_id
    INNER JOIN producto pro ON pro.id = dp.producto_id
    GROUP BY p.id, p.fecha_pedido, p.estado_pedido, c.id, csr.id
    ORDER BY p.fecha_pedido DESC
    """, nativeQuery = true)
    Page<String> buscarTodosLosPedidos(Pageable pegable);


}
