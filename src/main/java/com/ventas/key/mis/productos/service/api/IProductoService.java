package com.ventas.key.mis.productos.service.api;

import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.models.ICrud;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ProductoDTO;
import com.ventas.key.mis.productos.models.ProductoDetalle;

import java.util.List;
import java.util.Optional;



public interface IProductoService extends ICrud<
                                                Producto,
                                                List<Producto>, 
                                                Optional<Producto>, 
                                                Integer,
                                                PginaDto<List<Producto>>> {

    Producto actualizarStock(Integer id, Integer nuevoStock);

    PginaDto<List<ProductoDTO>> getAll(int size, int page);

    PginaDto<List<ProductoDTO>> findNombreOrCodigoBarra(int size, int page, String nombre);

    Producto saveProductoLote(ProductoDetalle producto) throws Exception;


}
