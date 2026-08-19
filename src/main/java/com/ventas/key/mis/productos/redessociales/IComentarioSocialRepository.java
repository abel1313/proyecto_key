package com.ventas.key.mis.productos.redessociales;

import com.ventas.key.mis.productos.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IComentarioSocialRepository extends BaseRepository<ComentarioSocial, Integer> {

    Optional<ComentarioSocial> findByCommentId(String commentId);

    boolean existsByAutorId(String autorId);
}
