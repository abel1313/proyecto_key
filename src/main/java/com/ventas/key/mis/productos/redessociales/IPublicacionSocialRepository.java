package com.ventas.key.mis.productos.redessociales;

import com.ventas.key.mis.productos.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

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
}
