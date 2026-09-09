package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.BoletoRifa;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IBoletoRifaRepository extends BaseRepository<BoletoRifa, Integer> {

    @Query("SELECT b FROM BoletoRifa b WHERE b.concursante.id = :concursanteId ORDER BY b.fecha DESC, b.id DESC")
    List<BoletoRifa> findByConcursanteId(@Param("concursanteId") Integer concursanteId);

    @Query("SELECT b FROM BoletoRifa b JOIN FETCH b.concursante c "
            + "WHERE c.configurarRifa.id = :rifaId ORDER BY b.id ASC")
    List<BoletoRifa> findByRifaId(@Param("rifaId") Integer rifaId);

    @Query("SELECT b FROM BoletoRifa b JOIN FETCH b.concursante c "
            + "WHERE c.configurarRifa.id = :rifaId AND b.descartado = false ORDER BY b.id ASC")
    List<BoletoRifa> findEnJuegoByRifaId(@Param("rifaId") Integer rifaId);

    @Modifying
    @Query("UPDATE BoletoRifa b SET b.descartado = false WHERE b.concursante.id IN "
            + "(SELECT c.id FROM Concursante c WHERE c.configurarRifa.id = :rifaId)")
    void reactivarTodosPorRifa(@Param("rifaId") Integer rifaId);
}
