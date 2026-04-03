package com.ventas.key.mis.productos.hexagonal.aplicacion;

import com.ventas.key.mis.productos.hexagonal.dominio.ProductoImagen;

import java.util.List;
import java.util.Optional;

public interface ImagenesProductoCasoUso {


    void create(ProductoImagen productoImagen);
    void update(ProductoImagen productoImagen) throws Exception;
    void delete(Integer idProductoImagen) throws Exception;
    void createAll(List<ProductoImagen> productoImagen);
    Optional<ProductoImagen> findById(Integer id) throws Exception;
}
