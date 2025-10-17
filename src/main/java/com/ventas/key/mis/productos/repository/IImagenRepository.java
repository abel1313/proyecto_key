package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Imagen;
import com.ventas.key.mis.productos.models.ImagenProductoResult;
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



}
