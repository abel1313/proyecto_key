package com.ventas.key.mis.productos.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ventas.key.mis.productos.entity.Rifa;

@Repository
public interface IRifaRepository extends BaseRepository<Rifa,Integer>{

    @Query(value = "SELECT * FROM rifas WHERE fechaRifa BETWEEN :inicio AND :fin AND palabra_Rifa = :palabraRifa", nativeQuery = true)

    List<Rifa> buscarPorRangoDeHora(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin, @Param("palabraRifa") String palabraRifa);

}
