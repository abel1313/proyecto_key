package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.RamoArmado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IRamoArmadoRepository extends BaseRepository<RamoArmado, Integer> {

    @Query("SELECT DISTINCT r FROM RamoArmado r LEFT JOIN FETCH r.accesorios a LEFT JOIN FETCH a.accesorio WHERE r.id = :id")
    Optional<RamoArmado> findByIdConAccesorios(@Param("id") Integer id);

    @Query(value = "SELECT DISTINCT r FROM RamoArmado r LEFT JOIN FETCH r.accesorios a LEFT JOIN FETCH a.accesorio ORDER BY r.id DESC",
           countQuery = "SELECT COUNT(r) FROM RamoArmado r")
    Page<RamoArmado> findAllConAccesorios(Pageable pageable);

    @Query(value = "SELECT DISTINCT r FROM RamoArmado r LEFT JOIN FETCH r.accesorios a LEFT JOIN FETCH a.accesorio WHERE r.activo = true ORDER BY r.id DESC",
           countQuery = "SELECT COUNT(r) FROM RamoArmado r WHERE r.activo = true")
    Page<RamoArmado> findActivosConAccesorios(Pageable pageable);
}
