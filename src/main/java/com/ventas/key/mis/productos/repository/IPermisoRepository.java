package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Permiso;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IPermisoRepository extends BaseRepository<Permiso, Integer> {

    Optional<Permiso> findByNombrePermiso(String nombrePermiso);
}