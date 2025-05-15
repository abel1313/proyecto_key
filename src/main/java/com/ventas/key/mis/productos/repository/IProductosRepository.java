package com.ventas.key.mis.productos.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.ventas.key.mis.productos.entity.Producto;

@Repository
public interface IProductosRepository extends BaseRepository<Producto,Integer>{
    Page<Producto> findByNombreContaining(String nombre, Pageable pageable); // 🔍 Busca por nombre
    Page<Producto> findByCodigoBarras_CodigoBarrasContaining(String codigoBarras, Pageable pageable); // 🔍 Busca por código de barras
    Optional<Producto> findByCodigoBarras_CodigoBarrasAndNombre(String codigoBarras, String nombre); // 🔍 Busca por código de barras
    Optional<Producto> findByCodigoBarras_CodigoBarras(String codigoBarras); // 🔍 Busca por código de barras




    


}
