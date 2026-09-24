package com.ventas.key.mis.productos.redessociales;

import com.ventas.key.mis.productos.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IMensajePausaRepository extends BaseRepository<MensajePausa, Integer> {

    boolean existsByAutorId(String autorId);

    Optional<MensajePausa> findByAutorId(String autorId);
}
