package com.ventas.key.mis.productos.redessociales;

import com.ventas.key.mis.productos.repository.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IComentarioPausaRepository extends BaseRepository<ComentarioPausa, Integer> {

    boolean existsByAutorIdAndPostId(String autorId, String postId);
}
