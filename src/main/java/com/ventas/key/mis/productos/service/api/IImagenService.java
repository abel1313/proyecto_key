package com.ventas.key.mis.productos.service.api;

import com.ventas.key.mis.productos.entity.Imagen;
import com.ventas.key.mis.productos.entity.LotesProductos;
import com.ventas.key.mis.productos.models.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface IImagenService extends ICrud<
                                    Imagen,
                                    List<Imagen>,
                                    Optional<Imagen>,
                                    Long,
                                    PginaDto<List<Imagen>>> {

    List<Imagen> saveAll(List<Imagen> list);
    List<ImagenProductoResult> findIdsImagenesProducto(List<Integer> list);

    /** @deprecated usar findImagenPrincipalPorProductoIdsV2 que obtiene bytes del micro de imágenes */
    @Deprecated
    PageableDto<List<ImagenProductoBase64>> findImagenPrincipalPorProductoIds(@Param("productoIds") Integer productoIds, int page, int size);

    PageableDto<List<ImagenProductoBase64>> findImagenPrincipalPorProductoIdsV2(Integer productoId, int page, int size);

    /** @deprecated usar deleteByIdV2 que también elimina del micro de imágenes */
    @Deprecated
    void deleteById(Long id);

    void deleteByIdV2(Long id);

    void deleteByIds(List<Long> ids);

    com.ventas.key.mis.productos.hexagonal.dominio.Imagen findByIdImg(Integer id) throws IOException;
    com.ventas.key.mis.productos.hexagonal.dominio.Imagen findByImagenId(Long imagenId) throws IOException;



}
