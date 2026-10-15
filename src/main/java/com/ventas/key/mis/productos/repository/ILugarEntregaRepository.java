package com.ventas.key.mis.productos.repository;

import java.util.List;

import com.ventas.key.mis.productos.entity.LugarEntrega;
import org.springframework.stereotype.Repository;

@Repository
public interface ILugarEntregaRepository extends BaseRepository<LugarEntrega, Integer> {

    // La fila del local ("recoger en tienda"); tiene que haber como mucho una.
    List<LugarEntrega> findByEsRecogerEnTiendaTrue();
}
