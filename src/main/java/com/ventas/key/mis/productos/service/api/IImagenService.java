package com.ventas.key.mis.productos.service.api;

import com.ventas.key.mis.productos.entity.Imagen;
import com.ventas.key.mis.productos.entity.LotesProductos;
import com.ventas.key.mis.productos.models.ICrud;
import com.ventas.key.mis.productos.models.ImagenProductoResult;
import com.ventas.key.mis.productos.models.PginaDto;

import java.util.List;
import java.util.Optional;

public interface IImagenService extends ICrud<
                                    Imagen,
                                    List<Imagen>,
                                    Optional<Imagen>,
                                    Integer,
                                    PginaDto<List<Imagen>>> {

    List<Imagen> saveAll(List<Imagen> list);
    List<ImagenProductoResult> findIdsImagenesProducto(List<Integer> list);
}
