package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.HistorialAcceso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IHistorialAccesoRepository extends BaseRepository<HistorialAcceso, Long> {

    Optional<HistorialAcceso> findBySessionId(String sessionId);

    @Modifying
    @Query("UPDATE HistorialAcceso h SET h.ultimaActividad = :ahora WHERE h.sessionId = :sessionId")
    void actualizarUltimaActividad(@Param("sessionId") String sessionId, @Param("ahora") LocalDateTime ahora);

    // desde/hasta tri-state (null = sin filtro por ese extremo), mismo patron que
    // buscarVariantesAdmin/buscarProductosAdmin. Mas reciente primero.
    @Query("SELECT h FROM HistorialAcceso h WHERE (:desde IS NULL OR h.fechaLogin >= :desde) "
            + "AND (:hasta IS NULL OR h.fechaLogin <= :hasta) ORDER BY h.fechaLogin DESC")
    Page<HistorialAcceso> buscarPorRangoFecha(@Param("desde") LocalDateTime desde,
                                               @Param("hasta") LocalDateTime hasta,
                                               Pageable pageable);
}
