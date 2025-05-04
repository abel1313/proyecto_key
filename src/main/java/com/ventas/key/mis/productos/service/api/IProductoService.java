package com.ventas.key.mis.productos.service.api;

import java.util.List;
import java.util.Optional;

import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.models.ICrud;

public interface IProductoService extends ICrud<Producto,
                                                Producto,
                                                List<Producto>, 
                                                Optional<Producto>, 
                                                Integer> {

    Producto actualizarStock(Integer id, Integer nuevoStock);

}
