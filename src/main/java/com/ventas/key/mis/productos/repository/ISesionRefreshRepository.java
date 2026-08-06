package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.SesionRefresh;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ISesionRefreshRepository extends BaseRepository<SesionRefresh, Long> {

    Optional<SesionRefresh> findBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);

    /** Cierra todas las sesiones de un usuario — cambio de contrasena y reseteo (hallazgo 6). */
    @Modifying
    @Query("DELETE FROM SesionRefresh s WHERE s.usuarioId = :usuarioId")
    int deleteByUsuarioId(@Param("usuarioId") Integer usuarioId);

    /**
     * Barrido de sesiones ya vencidas. Sin esto la tabla solo crece: las sesiones que nunca pasan
     * por logout (el usuario simplemente cierra el navegador) quedarian ahi para siempre.
     */
    @Modifying
    @Query("DELETE FROM SesionRefresh s WHERE s.expira < :ahora")
    int deleteExpiradas(@Param("ahora") LocalDateTime ahora);
}
