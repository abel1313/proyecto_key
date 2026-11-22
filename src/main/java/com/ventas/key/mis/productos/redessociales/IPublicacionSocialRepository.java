package com.ventas.key.mis.productos.redessociales;

import com.ventas.key.mis.productos.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IPublicacionSocialRepository extends BaseRepository<PublicacionSocial, Integer> {

    Page<PublicacionSocial> findByVarianteIdOrderByIdDesc(Integer varianteId, Pageable pageable);

    List<PublicacionSocial> findByEstadoAndScheduledPublishTimeLessThanEqual(String estado, LocalDateTime hasta);

    // Usado por el bot de comentarios (FacebookCommentBotService) para saber si el post donde
    // comentaron tiene una variante asociada, y darle ese contexto al chatbot.
    Optional<PublicacionSocial> findByPostIdFacebook(String postIdFacebook);

    // ── WhatsApp (hexagonal/whatsapp) ─────────────────────────────────────────────────────────
    // PENDIENTE con hora vencida (o sin hora: "publicar ahora"). El bloqueo evita que dos llamadas del bot recojan la misma.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PublicacionSocial p WHERE p.plataforma = 'whatsapp' AND p.estado = 'PENDIENTE' "
            + "AND (p.scheduledPublishTime IS NULL OR p.scheduledPublishTime <= :ahora) "
            + "ORDER BY p.scheduledPublishTime ASC, p.id ASC")
    List<PublicacionSocial> pendientesWhatsappConBloqueo(@Param("ahora") LocalDateTime ahora, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PublicacionSocial p WHERE p.id = :id AND p.plataforma = 'whatsapp'")
    Optional<PublicacionSocial> buscarWhatsappConBloqueo(@Param("id") Integer id);

    @Query("SELECT p FROM PublicacionSocial p WHERE p.id = :id AND p.plataforma = 'whatsapp'")
    Optional<PublicacionSocial> buscarWhatsapp(@Param("id") Integer id);
}
