package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.LugarEntregaAnillo;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ILugarEntregaAnilloRepository extends BaseRepository<LugarEntregaAnillo, Integer> {

    List<LugarEntregaAnillo> findByLugarEntregaIdOrderByRadioMetrosAsc(Integer lugarEntregaId);
}
