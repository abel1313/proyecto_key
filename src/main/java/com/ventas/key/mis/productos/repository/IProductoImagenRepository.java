package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Imagen;
import com.ventas.key.mis.productos.entity.ProductoImagen;
import com.ventas.key.mis.productos.models.ImagenUpdateDto;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IProductoImagenRepository extends BaseRepository<ProductoImagen,Integer>{

    List<ProductoImagen> findByProductoId(Integer productoId);

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
