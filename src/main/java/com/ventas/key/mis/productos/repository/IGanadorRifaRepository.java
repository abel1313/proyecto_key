package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.GanadorRifa;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface IGanadorRifaRepository extends BaseRepository<GanadorRifa, Integer> {

    @Query("SELECT g FROM GanadorRifa g WHERE g.configurarRifaVariante.configurarRifa.id = :rifaId")
    List<GanadorRifa> findByConfigurarRifaId(@Param("rifaId") Integer rifaId);

    @Query("SELECT COUNT(g) FROM GanadorRifa g WHERE g.configurarRifaVariante.configurarRifa.id = :rifaId AND g.descartado = false")
    long countGanadoresByRifaId(@Param("rifaId") Integer rifaId);

    @Query("SELECT COUNT(g) FROM GanadorRifa g WHERE g.configurarRifaVariante.id = :varianteRifaId AND g.descartado = true")
    long countDescartadosByVarianteRifaId(@Param("varianteRifaId") Integer varianteRifaId);

    @Query("SELECT g FROM GanadorRifa g JOIN FETCH g.configurarRifaVariante crv JOIN FETCH g.concursante WHERE crv.configurarRifa.id = :rifaId AND g.descartado = false")
    List<GanadorRifa> findGanadoresByRifaId(@Param("rifaId") Integer rifaId);

    @Modifying
    @Transactional
    @Query("DELETE FROM GanadorRifa g WHERE g.configurarRifaVariante.configurarRifa.id = :rifaId")
    void deleteByRifaId(@Param("rifaId") Integer rifaId);
}