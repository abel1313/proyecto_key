package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Imagen;
import com.ventas.key.mis.productos.entity.ProductoImagen;
import com.ventas.key.mis.productos.models.ImagenProductoDto;
import com.ventas.key.mis.productos.models.ImagenProductoResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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
    SELECT new com.ventas.key.mis.productos.models.ImagenProductoResult(
        pi.producto.id, MIN(pi.imagen.id)
    )
    FROM ProductoImagen pi
    WHERE pi.producto.id = :productoIds
    GROUP BY pi.producto.id
""")
    Optional<ImagenProductoResult> findImagenByImg(@Param("productoIds") int idProducto);


    @Query(value = """
        SELECT new com.ventas.key.mis.productos.models.ImagenProductoDto(
            pi.producto.id,
            pi.imagen.id,
            pi.producto.nombre,
            pi.producto.precioVenta,
            pi.imagen.extension,
            pi.imagen.base64,
            CASE
                WHEN pi.producto.stock < 2 THEN 'LOWSTOCK'
                ELSE 'INSTOCK'
            END)
        FROM ProductoImagen pi
        WHERE pi.producto.id = :id
        """)
    List<ImagenProductoDto> findImagenPrincipalPorProductoIds55(@Param("id") Integer id);

    @Query(value = """
        SELECT new com.ventas.key.mis.productos.models.ImagenProductoDto(
            pi.producto.id,
            pi.imagen.id,
            pi.producto.nombre,
            pi.producto.precioVenta,
            pi.imagen.extension,
            pi.imagen.base64,
            CASE
                WHEN pi.producto.stock < 2 THEN 'LOWSTOCK'
                ELSE 'INSTOCK'
            END)
        FROM ProductoImagen pi
        WHERE pi.producto.id = :id
        """,
            countQuery = """
        SELECT COUNT(pi)
        FROM ProductoImagen pi
        WHERE pi.producto.id = :id
        """)
    Page<ImagenProductoDto> findImagenPrincipalPorProductoIds(@Param("id") Integer id, Pageable pageable);


//    @Query("""
//    SELECT pi.producto,pi.imagen
//    FROM ProductoImagen pi
//    WHERE pi.producto.id = :id
//""")
//    List<ProductoImagen> findImagenPrincipalPorProductoIds1(@Param("id") Integer id, Pageable pageable);




}
