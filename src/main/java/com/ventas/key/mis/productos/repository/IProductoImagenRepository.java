package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Imagen;
import com.ventas.key.mis.productos.entity.ProductoImagen;
import com.ventas.key.mis.productos.models.ImagenUpdateDto;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * @deprecated Migrar a micro_imagenes. Gestiona la relación producto-imagen que debe
 * vivir en el microservicio de imágenes. No agregar nueva lógica aquí.
 */
@Deprecated
public interface IProductoImagenRepository extends BaseRepository<ProductoImagen,Integer>{

    @Query("SELECT pi FROM ProductoImagen pi JOIN FETCH pi.imagen WHERE pi.producto.id = :productoId")
    List<ProductoImagen> findByProductoId(@Param("productoId") Integer productoId);

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
                        PI.imagen.nombreImagen,
                        PI.principal
                        )
            FROM ProductoImagen PI
                WHERE PI.producto.id = :productoId
            """
    )
    List<ImagenUpdateDto>getImagenByProductoId(Integer productoId);

    List<ProductoImagen> findAllByProductoId(Integer productoId);

    @Query("SELECT pi FROM ProductoImagen pi WHERE pi.producto.id IN :productoIds ORDER BY CASE WHEN pi.principal = true THEN 0 ELSE 1 END ASC, pi.imagen.id ASC")
    List<ProductoImagen> findPrimeraImagenByProductoIdIn(@Param("productoIds") List<Integer> productoIds);

    @Query("SELECT pi FROM ProductoImagen pi WHERE pi.producto.id = :productoId AND pi.imagen.id = :imagenId")
    Optional<ProductoImagen> findByProductoIdAndImagenId(@Param("productoId") Integer productoId, @Param("imagenId") Long imagenId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ProductoImagen pi WHERE pi.imagen.id IN :imagenIds")
    void deleteByImagenIdIn(@Param("imagenIds") List<Long> imagenIds);

    @Modifying
    @Query("UPDATE ProductoImagen pi SET pi.principal = false WHERE pi.producto.id = :productoId")
    void desmarcarTodosPrincipal(@Param("productoId") Integer productoId);

    @Modifying
    @Query("UPDATE ProductoImagen pi SET pi.principal = true WHERE pi.imagen.id = :imagenId AND pi.producto.id = :productoId")
    void marcarComoPrincipal(@Param("imagenId") Long imagenId, @Param("productoId") Integer productoId);
}
