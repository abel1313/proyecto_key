package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.MpPaymentIntent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IMpPaymentIntentRepository extends BaseRepository<MpPaymentIntent, Integer> {

    Optional<MpPaymentIntent> findByIntentId(String intentId);

    Page<MpPaymentIntent> findByPedidoId(Integer pedidoId, Pageable pageable);

    Page<MpPaymentIntent> findByEstado(String estado, Pageable pageable);

    Optional<MpPaymentIntent> findFirstByPedidoIdAndEstadoOrderByFechaCreacionDesc(Integer pedidoId, String estado);

    @Query("SELECT COUNT(m) FROM MpPaymentIntent m WHERE m.pedidoId = :pedidoId AND m.estado IN :estados AND m.fechaCreacion >= :desde")
    long countIntentosFallidosRecientes(@Param("pedidoId") Integer pedidoId,
                                        @Param("estados") List<String> estados,
                                        @Param("desde") LocalDateTime desde);
}