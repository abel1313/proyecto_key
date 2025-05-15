package com.ventas.key.mis.productos.service.api;

import java.util.List;
import java.util.Optional;

import com.ventas.key.mis.productos.entity.LotesProductos;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.models.ICrud;
import com.ventas.key.mis.productos.models.PginaDto;

public interface ILoteProductoService extends ICrud<
                                                LotesProductos,
                                                List<LotesProductos>, 
                                                Optional<LotesProductos>, 
                                                Integer,
                                                PginaDto<List<LotesProductos>>>{

}
