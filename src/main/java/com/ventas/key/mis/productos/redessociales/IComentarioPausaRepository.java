package com.ventas.key.mis.productos.redessociales;

import com.ventas.key.mis.productos.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IComentarioPausaRepository extends BaseRepository<ComentarioPausa, Integer> {

    boolean existsByAutorIdAndPostId(String autorId, String postId);

    Optional<ComentarioPausa> findFirstByAutorIdAndPostId(String autorId, String postId);
}
