package com.ventas.key.mis.productos.redessociales;

import com.ventas.key.mis.productos.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IComentarioSocialRepository extends BaseRepository<ComentarioSocial, Integer> {

    Optional<ComentarioSocial> findByCommentId(String commentId);

    boolean existsByAutorId(String autorId);

    // Sirve para reconocer, cuando Meta reenvia por webhook un comentario del cual la Pagina es
    // autora, si es un eco de nuestro propio bot (ya guardado aqui) o una respuesta manual del
    // admin (no encontrado -> ver FacebookCommentBotService.detectarRespuestaManualYPausar).
    boolean existsByRespuestaCommentId(String respuestaCommentId);
}
