package com.ventas.key.mis.productos.service.api;

import java.util.List;
import java.util.Optional;

import com.ventas.key.mis.productos.entity.Gastos;
import com.ventas.key.mis.productos.models.ICrud;

public interface IGastosService extends ICrud<
                                                Gastos,
                                                List<Gastos>, 
                                                Optional<Gastos>, 
                                                Integer>{


    Gastos saveGastosNT(Gastos gastos);

}
