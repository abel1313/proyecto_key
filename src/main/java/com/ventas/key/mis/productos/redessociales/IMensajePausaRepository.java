package com.ventas.key.mis.productos.redessociales;

import com.ventas.key.mis.productos.repository.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IMensajePausaRepository extends BaseRepository<MensajePausa, Integer> {

    boolean existsByAutorId(String autorId);
}
