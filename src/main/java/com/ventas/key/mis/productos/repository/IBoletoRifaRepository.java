package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.BoletoRifa;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IBoletoRifaRepository extends BaseRepository<BoletoRifa, Integer> {

    @Query("SELECT b FROM BoletoRifa b WHERE b.concursante.id = :concursanteId ORDER BY b.fecha DESC, b.id DESC")
    List<BoletoRifa> findByConcursanteId(@Param("concursanteId") Integer concursanteId);
}
