package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.ProductoImagen;

import java.util.List;

public interface IProductoImagenRepository extends BaseRepository<ProductoImagen,Integer>{

    List<ProductoImagen> findByProductoId(Integer productoId);
}
