package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Concursante;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface IConcursanteRepository extends BaseRepository<Concursante, Integer> {

    List<Concursante> findByConfigurarRifaId(Integer configurarRifaId);

    List<Concursante> findByConfigurarRifaIdAndDescartadoFalse(Integer configurarRifaId);

    List<Concursante> findByConfigurarRifaIdAndDescartadoFalseAndPalabraClave(
            Integer configurarRifaId, String palabraClave);

    List<Concursante> findByConfigurarRifaIdAndPalabraClave(
            Integer configurarRifaId, String palabraClave);

    @Modifying
    @Transactional
    void deleteByConfigurarRifaId(Integer configurarRifaId);
}