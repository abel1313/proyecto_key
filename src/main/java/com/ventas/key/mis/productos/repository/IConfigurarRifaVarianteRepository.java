package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.ConfigurarRifaVariante;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IConfigurarRifaVarianteRepository extends BaseRepository<ConfigurarRifaVariante, Integer> {

    @Query("SELECT crv FROM ConfigurarRifaVariante crv JOIN FETCH crv.variante v JOIN FETCH v.producto WHERE crv.configurarRifa.id = :configurarRifaId ORDER BY crv.orden ASC")
    List<ConfigurarRifaVariante> findByConfigurarRifaIdOrderByOrdenAsc(@Param("configurarRifaId") Integer configurarRifaId);

    @Query("SELECT v.palabraClave FROM ConfigurarRifaVariante v WHERE v.configurarRifa.id = :rifaId ORDER BY v.orden ASC")
    List<String> findPalabrasClave(@Param("rifaId") Integer rifaId);

    boolean existsByConfigurarRifaIdAndPalabraClave(Integer configurarRifaId, String palabraClave);
}