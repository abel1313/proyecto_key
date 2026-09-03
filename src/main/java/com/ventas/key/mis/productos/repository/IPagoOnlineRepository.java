package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.PagoOnline;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IPagoOnlineRepository extends BaseRepository<PagoOnline, Integer> {

    Optional<PagoOnline> findByProveedorAndReferenciaExterna(String proveedor, String referenciaExterna);

    // El pedido de prueba pudo haber intentado mas de una pasarela -- filtrar por proveedor
    // ademas de pedidoId evita agarrar por error el intento de la OTRA pasarela mas reciente.
    Optional<PagoOnline> findFirstByProveedorAndPedidoIdOrderByFechaCreacionDesc(String proveedor, Integer pedidoId);

    Page<PagoOnline> findByPedidoIdOrderByFechaCreacionDesc(Integer pedidoId, Pageable pageable);

    Page<PagoOnline> findByEstadoOrderByFechaCreacionDesc(String estado, Pageable pageable);
}
