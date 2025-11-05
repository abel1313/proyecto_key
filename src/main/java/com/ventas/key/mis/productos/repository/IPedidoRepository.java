package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IPedidoRepository extends BaseRepository<Pedido,Integer>{


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
          'fecha_pedido', DATE_FORMAT(p.fecha_pedido, '%d/%m/%Y'),
          'estado_pedido', p.estado_pedido,
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
    INNER JOIN detalle_pedido dp ON p.id = dp.pedido_id
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
          'fecha_pedido', DATE_FORMAT(p.fecha_pedido, '%d/%m/%Y'),
          'estado_pedido', p.estado_pedido,
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
    INNER JOIN detalle_pedido dp ON p.id = dp.pedido_id
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
          'fecha_pedido', DATE_FORMAT(p.fecha_pedido, '%d/%m/%Y'),
          'estado_pedido', p.estado_pedido,
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
    INNER JOIN detalle_pedido dp ON p.id = dp.pedido_id
    INNER JOIN clientes c ON c.id = p.cliente_id
        INNER JOIN producto pro
        on pro.id = dp.producto_id 
    WHERE c.id = :idCliente and p.id = :idPedido
    GROUP BY p.id, p.fecha_pedido, p.estado_pedido, c.id
    ORDER BY p.fecha_pedido DESC
    """, nativeQuery = true)
    Page<String> pediodPorId(@Param("idPedido") int idPedido,@Param("idCliente") int idCliente, Pageable pegable);

    @Query(value = """

            SELECT\s
      JSON_OBJECT(
        'cliente', JSON_OBJECT(
       	'id', c.id,
       	'nombreCliente', c.nombre_persona,
       	'correoElectronico', c.correo_electronico,
       	'numeroTelefonico', c.numero_telefonico\s
        ),
        'pedido', JSON_OBJECT(
          'id', p.id,
          'fecha_pedido', DATE_FORMAT(p.fecha_pedido, '%d/%m/%Y'),
          'estado_pedido', p.estado_pedido,
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
    INNER JOIN detalle_pedido dp ON p.id = dp.pedido_id
    INNER JOIN clientes c ON c.id = p.cliente_id
        INNER JOIN producto pro
        on pro.id = dp.producto_id\s
    WHERE c.nombre_persona LIKE '%"+:buscar+"%' OR c.correo_electronico LIKE '%"+:buscar+"%' or c.numero_telefonico LIKE '%"+:buscar+"%'
    GROUP BY p.id, p.fecha_pedido, p.estado_pedido, c.id
    ORDER BY p.fecha_pedido DESC
    """, nativeQuery = true)
    Page<String> buscarPedidosPorCliente(@Param("buscar") String buscar, Pageable pegable);


    @Query(value = """

            SELECT\s
      JSON_OBJECT(
        'cliente', JSON_OBJECT(
       	'id', c.id,
       	'nombreCliente', c.nombre_persona,
       	'correoElectronico', c.correo_electronico,
       	'numeroTelefonico', c.numero_telefonico\s
        ),
        'pedido', JSON_OBJECT(
          'id', p.id,
          'fecha_pedido', DATE_FORMAT(p.fecha_pedido, '%d/%m/%Y'),
          'estado_pedido', p.estado_pedido,
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
    INNER JOIN detalle_pedido dp ON p.id = dp.pedido_id
    INNER JOIN clientes c ON c.id = p.cliente_id
        INNER JOIN producto pro
        on pro.id = dp.producto_id\s
    GROUP BY p.id, p.fecha_pedido, p.estado_pedido, c.id
    ORDER BY p.fecha_pedido DESC
    """, nativeQuery = true)
    Page<String> buscarTodosLosPedidos(Pageable pegable);


}
