package com.ventas.key.mis.productos.service.api;

import com.ventas.key.mis.productos.entity.Imagen;
import com.ventas.key.mis.productos.entity.ProductoImagen;
import com.ventas.key.mis.productos.models.ICrud;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ProductoImagenDto;

import java.util.List;
import java.util.Optional;

public interface IProductoImagenService extends ICrud<
        ProductoImagen,
        List<ProductoImagen>,
        Optional<ProductoImagen>,
        Integer,
        PginaDto<List<ProductoImagen>>> {

    List<ProductoImagen> saveAll(List<ProductoImagen> productoImagen);

    List<ProductoImagen> findByProductoId(Integer productoId);

    /** @deprecated usar findByImagenesPorIdProductoV2 que apunta a /imagen/v1/file/ */
    @Deprecated
    ProductoImagenDto findByImagenesPorIdProducto(Integer productoId);

    ProductoImagenDto findByImagenesPorIdProductoV2(Integer productoId);

    void eliminarImagenesDeProductos(List<Integer> productoIds);
    void eliminarImagenesEspecificas(Integer productoId, List<Long> imagenIds);
}
