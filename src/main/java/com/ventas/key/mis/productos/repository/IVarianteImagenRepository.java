package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.productoVariantes.VarianteImagen;
import com.ventas.key.mis.productos.models.ImagenUpdateDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IVarianteImagenRepository extends BaseRepository<VarianteImagen, Integer> {

    List<VarianteImagen> findByVarianteId(Integer varianteId);

    @Query("""
            SELECT new com.ventas.key.mis.productos.models.ImagenUpdateDto(
                        VI.imagen.id,
                        VI.imagen.base64,
                        VI.imagen.extension,
                        VI.imagen.nombreImagen
                        )
            FROM VarianteImagen VI
                WHERE VI.variante.id = :varianteId
            """)
    List<ImagenUpdateDto> getImagenByVarianteId(@Param("varianteId") Integer varianteId);

    Page<VarianteImagen> findByVarianteId(Integer varianteId, Pageable pageable);

    @Query("""
            SELECT vi FROM VarianteImagen vi
            WHERE vi.variante.producto.id = :productoId
              AND LOWER(vi.variante.color) = LOWER(:color)
              AND vi.variante.id <> :excluirId
            """)
    List<VarianteImagen> findImagenesPorProductoIdYColor(
            @Param("productoId") Integer productoId,
            @Param("color") String color,
            @Param("excluirId") Integer excluirId);

    @Query("SELECT vi FROM VarianteImagen vi WHERE vi.variante.id IN :varianteIds ORDER BY vi.id ASC")
    List<VarianteImagen> findByVarianteIdIn(@Param("varianteIds") List<Integer> varianteIds);
}