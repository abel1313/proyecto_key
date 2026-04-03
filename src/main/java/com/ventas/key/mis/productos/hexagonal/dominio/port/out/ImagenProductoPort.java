package com.ventas.key.mis.productos.hexagonal.dominio.port.out;

import com.ventas.key.mis.productos.hexagonal.dominio.Imagen;
import com.ventas.key.mis.productos.hexagonal.dominio.ProductoImagen;
import com.ventas.key.mis.productos.hexagonal.dominio.mapper.RequestProductoImagen;
import com.ventas.key.mis.productos.models.ResponseGeneric;

import java.util.List;

public interface ImagenProductoPort {


    ResponseGeneric<ProductoImagen> save(RequestProductoImagen requestProductoImagen);


    ResponseGeneric<ProductoImagen> saveAll(List<RequestProductoImagen> requestProductoImagen);


    ResponseGeneric<ProductoImagen> update(RequestProductoImagen requestProductoImagen) throws Exception;


    ResponseGeneric<ProductoImagen> update(Integer id) throws Exception;


    ResponseGeneric<ProductoImagen> findById(Integer id) throws Exception;

    Imagen buscarImagenProducto(Integer id) throws Exception;

    }
