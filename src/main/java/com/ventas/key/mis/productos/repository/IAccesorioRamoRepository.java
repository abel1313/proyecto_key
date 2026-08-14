package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.AccesorioRamo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IAccesorioRamoRepository extends BaseRepository<AccesorioRamo, Integer> {

    // Solo deberia existir un accesorio activo marcado como "es papel" a la vez -- si en algun
    // momento se necesita mas de uno, este metodo hay que revisarlo (findFirst en vez de exactamente uno).
    Optional<AccesorioRamo> findFirstByEsPapelTrueAndActivoTrue();

    // Usado por AccesorioRamoServiceImpl.save() para impedir un segundo "es papel" activo --
    // excluye el propio id para no bloquearse a si mismo al editar.
    Optional<AccesorioRamo> findFirstByEsPapelTrueAndActivoTrueAndIdNot(Integer id);
}
