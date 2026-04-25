package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.MpPaymentIntent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IMpPaymentIntentRepository extends BaseRepository<MpPaymentIntent, Integer> {

    Optional<MpPaymentIntent> findByIntentId(String intentId);

    Page<MpPaymentIntent> findByPedidoId(Integer pedidoId, Pageable pageable);

    Page<MpPaymentIntent> findByEstado(String estado, Pageable pageable);
}