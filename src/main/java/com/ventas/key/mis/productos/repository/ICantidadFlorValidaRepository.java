package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.CantidadFlorValida;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ICantidadFlorValidaRepository extends BaseRepository<CantidadFlorValida, Integer> {

    @Query("SELECT c FROM CantidadFlorValida c WHERE c.tipoFlor.id = :tipoFlorId AND c.activo = true ORDER BY c.cantidad ASC")
    List<CantidadFlorValida> findActivasPorTipoFlorOrdenadas(@Param("tipoFlorId") Integer tipoFlorId);

    Optional<CantidadFlorValida> findByTipoFlorIdAndCantidadAndActivoTrue(Integer tipoFlorId, Integer cantidad);
}
