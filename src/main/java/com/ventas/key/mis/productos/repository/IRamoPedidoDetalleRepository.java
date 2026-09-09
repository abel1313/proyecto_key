package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.RamoPedidoDetalle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IRamoPedidoDetalleRepository extends BaseRepository<RamoPedidoDetalle, Integer> {
    List<RamoPedidoDetalle> findByPedidoId(Integer pedidoId);

    @Query(value = "SELECT r FROM RamoPedidoDetalle r JOIN FETCH r.pedido p LEFT JOIN FETCH p.cliente "
                  + "WHERE r.fraseListonEstado = 'PENDIENTE_VALIDACION' ORDER BY r.fechaCreacion ASC",
           countQuery = "SELECT COUNT(r) FROM RamoPedidoDetalle r WHERE r.fraseListonEstado = 'PENDIENTE_VALIDACION'")
    Page<RamoPedidoDetalle> findFrasesPendientes(Pageable pageable);
}
