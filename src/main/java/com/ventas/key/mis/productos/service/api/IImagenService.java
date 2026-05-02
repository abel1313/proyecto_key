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

    PageableDto<List<ImagenProductoBase64>> findImagenPrincipalPorProductoIds(@Param("productoIds") Integer productoIds, int page, int size);
    void deleteById(Long id);
    void deleteByIds(List<Long> ids);

    com.ventas.key.mis.productos.hexagonal.dominio.Imagen findByIdImg(Integer id) throws IOException;



}
