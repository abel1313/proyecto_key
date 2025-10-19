package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Imagen;
import com.ventas.key.mis.productos.entity.ProductoImagen;
import com.ventas.key.mis.productos.models.ImagenProductoResult;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IImagenRepository extends BaseRepository<Imagen,Integer>{

    @Query("""
    SELECT new com.ventas.key.mis.productos.models.ImagenProductoResult(
        pi.producto.id, MIN(pi.imagen.id)
    )
    FROM ProductoImagen pi
    WHERE pi.producto.id IN :productoIds
    GROUP BY pi.producto.id
""")
    List<ImagenProductoResult> findImagenPrincipalPorProductoIds(@Param("productoIds") List<Integer> productoIds);


    @Query("""
    SELECT pi.imagen
    FROM ProductoImagen pi
    WHERE pi.producto.id = :id
""")
    List<Imagen> findImagenPrincipalPorProductoIds(@Param("id") Integer id, Pageable pageable);

//    @Query("""
//    SELECT pi.producto,pi.imagen
//    FROM ProductoImagen pi
//    WHERE pi.producto.id = :id
//""")
//    List<ProductoImagen> findImagenPrincipalPorProductoIds1(@Param("id") Integer id, Pageable pageable);




}
