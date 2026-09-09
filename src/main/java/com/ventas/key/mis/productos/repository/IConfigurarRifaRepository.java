package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IConfigurarRifaRepository extends BaseRepository<ConfigurarRifa, Integer> {

    List<ConfigurarRifa> findByActivaTrue();

    // Publicada hay una sola: al marcar una se apaga la anterior de un solo golpe, sin
    // traerlas todas a memoria para guardarlas una por una.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ConfigurarRifa r SET r.publica = false WHERE r.publica = true AND r.id <> :id")
    void despublicarLasDemas(@Param("id") Integer id);

    @Query("SELECT r FROM ConfigurarRifa r WHERE r.activa = true AND r.fechaHoraLimite BETWEEN :inicio AND :fin")
    List<ConfigurarRifa> findActivasDelDia(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    List<ConfigurarRifa> findByFechaHoraLimiteBetween(LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT r FROM ConfigurarRifa r WHERE r.activa = true AND r.fechaHoraLimite < :limite")
    List<ConfigurarRifa> findActivasVencidas(@Param("limite") LocalDateTime limite);
}
