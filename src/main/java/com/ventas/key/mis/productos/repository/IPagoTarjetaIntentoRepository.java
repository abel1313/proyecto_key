package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.PagoTarjetaIntento;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface IPagoTarjetaIntentoRepository extends BaseRepository<PagoTarjetaIntento, Integer> {

    /** El mas reciente primero: si por algun error hubiera varios activos, se ve el ultimo. */
    Optional<PagoTarjetaIntento> findFirstByPedidoIdAndEstadoInOrderByCreadoEnDescIdDesc(
            Integer pedidoId, Collection<String> estados);

    long countByPedidoIdAndEstadoAndActualizadoEnGreaterThanEqual(
            Integer pedidoId, String estado, LocalDateTime desde);

    List<PagoTarjetaIntento> findByEstadoInAndCreadoEnBeforeOrderByCreadoEnAsc(
            Collection<String> estados, LocalDateTime limite);

    List<PagoTarjetaIntento> findByEstadoInAndActualizadoEnGreaterThanEqualOrderByActualizadoEnAsc(
            Collection<String> estados, LocalDateTime desde);

    Optional<PagoTarjetaIntento> findFirstByIdExterno(String idExterno);
}
