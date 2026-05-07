package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Concursante;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IConcursanteRepository extends BaseRepository<Concursante, Integer> {

    List<Concursante> findByConfigurarRifaId(Integer configurarRifaId);

    List<Concursante> findByConfigurarRifaIdAndDescartadoFalse(Integer configurarRifaId);

    List<Concursante> findByConfigurarRifaIdAndDescartadoFalseAndOrdenDesdeLessThanEqual(
            Integer configurarRifaId, int ordenDesde);

    void deleteByConfigurarRifaId(Integer configurarRifaId);
}