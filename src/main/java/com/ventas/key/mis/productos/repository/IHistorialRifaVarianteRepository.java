package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.HistorialRifaVariante;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface IHistorialRifaVarianteRepository extends BaseRepository<HistorialRifaVariante, Integer> {

    List<HistorialRifaVariante> findByConfigurarRifaIdOrderByOrdenAsc(Integer configurarRifaId);

    @Modifying
    @Transactional
    @Query("DELETE FROM HistorialRifaVariante h WHERE h.configurarRifa.id = :rifaId")
    void deleteByRifaId(@Param("rifaId") Integer rifaId);
}