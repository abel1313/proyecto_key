package com.ventas.key.mis.productos.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.ventas.key.mis.productos.entity.CodigoBarra;

@Repository
public interface ICodigoBarrasRepository extends BaseRepository<CodigoBarra,Integer> {


    Optional<CodigoBarra> findByCodigoBarras(String codigoBarra);

}
