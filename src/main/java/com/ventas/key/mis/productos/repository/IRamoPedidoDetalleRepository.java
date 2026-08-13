package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.RamoPedidoDetalle;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IRamoPedidoDetalleRepository extends BaseRepository<RamoPedidoDetalle, Integer> {
    List<RamoPedidoDetalle> findByPedidoId(Integer pedidoId);
}
