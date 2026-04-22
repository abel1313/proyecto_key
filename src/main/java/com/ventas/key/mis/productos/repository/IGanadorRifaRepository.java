package com.ventas.key.mis.productos.repository;

import org.springframework.stereotype.Repository;

import com.ventas.key.mis.productos.entity.GanadorRifa;

import java.util.List;
import java.util.Optional;

@Repository
public interface IGanadorRifaRepository extends BaseRepository<GanadorRifa, Integer> {

    List<GanadorRifa> findByConcursanteConfigurarRifaId(Integer configurarRifaId);

    Optional<GanadorRifa> findByConcursanteConfigurarRifaIdAndDescartadoFalse(Integer configurarRifaId);
}
