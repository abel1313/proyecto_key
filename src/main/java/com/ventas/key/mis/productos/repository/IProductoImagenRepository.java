package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Imagen;
import com.ventas.key.mis.productos.entity.ProductoImagen;
import com.ventas.key.mis.productos.models.ImagenUpdateDto;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IProductoImagenRepository extends BaseRepository<ProductoImagen,Integer>{

    List<ProductoImagen> findByProductoId(Integer productoId);

    @Query("SELECT pi.imagen.id FROM ProductoImagen pi WHERE pi.producto.id IN :productoIds")
    List<Long> findImagenIdsByProductoIdIn(@Param("productoIds") List<Integer> productoIds);

    @Modifying
    @Query("DELETE FROM ProductoImagen pi WHERE pi.producto.id IN :productoIds")
    void deleteByProductoIdIn(@Param("productoIds") List<Integer> productoIds);

    @Modifying
    @Query("DELETE FROM ProductoImagen pi WHERE pi.producto.id = :productoId AND pi.imagen.id IN :imagenIds")
    void deleteByProductoIdAndImagenIdIn(@Param("productoId") Integer productoId,
                                         @Param("imagenIds") List<Long> imagenIds);

    @Query(
            """
            SELECT new com.ventas.key.mis.productos.models.ImagenUpdateDto (
                        PI.imagen.id,
                        PI.imagen.base64,
                        PI.imagen.extension,
                        PI.imagen.nombreImagen
                        )
            FROM ProductoImagen PI
                WHERE PI.producto.id = :productoId
            """
    )
    List<ImagenUpdateDto>getImagenByProductoId(Integer productoId);
}
