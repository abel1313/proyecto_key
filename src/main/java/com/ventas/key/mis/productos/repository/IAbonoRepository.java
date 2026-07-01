package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.AbonoPedido;
import com.ventas.key.mis.productos.entity.Pedido;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IAbonoRepository extends BaseRepository<AbonoPedido, Integer> {

    List<AbonoPedido> findByPedidoIdOrderByFechaPagoAsc(int pedidoId);

    @Query("SELECT p FROM Pedido p WHERE p.tipoPedido IN ('APARTADO', 'FIADO') " +
           "AND p.estadoPedido NOT IN ('PAGADO', 'cancelado') ORDER BY p.fechaPedido DESC")
    List<Pedido> findPedidosConSaldo();

    @Query("SELECT p FROM Pedido p WHERE p.tipoPedido IN ('APARTADO', 'FIADO') " +
           "AND p.estadoPedido = 'PAGADO' ORDER BY p.fechaPedido DESC")
    List<Pedido> findPedidosPagados();

    @Query("SELECT p FROM Pedido p WHERE p.tipoPedido IN ('APARTADO', 'FIADO') " +
           "AND p.estadoPedido = 'cancelado' ORDER BY p.fechaCancelacion DESC")
    List<Pedido> findPedidosCancelados();
}
