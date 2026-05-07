package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.GanadorRifa;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IGanadorRifaRepository extends BaseRepository<GanadorRifa, Integer> {

    @Query("SELECT g FROM GanadorRifa g WHERE g.configurarRifaProducto.configurarRifa.id = :rifaId")
    List<GanadorRifa> findByConfigurarRifaId(@Param("rifaId") Integer configurarRifaId);

    @Query("SELECT g FROM GanadorRifa g WHERE g.configurarRifaProducto.id = :productoRifaId AND g.descartado = false")
    List<GanadorRifa> findGanadoresByProductoRifaId(@Param("productoRifaId") Integer productoRifaId);

    @Query("SELECT COUNT(g) FROM GanadorRifa g WHERE g.configurarRifaProducto.id = :productoRifaId AND g.descartado = true")
    long countDescartadosByProductoRifaId(@Param("productoRifaId") Integer productoRifaId);

    @Query("SELECT COUNT(g) FROM GanadorRifa g WHERE g.configurarRifaProducto.configurarRifa.id = :rifaId AND g.descartado = false")
    long countGanadoresByRifaId(@Param("rifaId") Integer configurarRifaId);

    @Query("SELECT g FROM GanadorRifa g WHERE g.configurarRifaProducto.configurarRifa.id = :rifaId AND g.descartado = false")
    List<GanadorRifa> findAllGanadoresByRifaId(@Param("rifaId") Integer configurarRifaId);
}