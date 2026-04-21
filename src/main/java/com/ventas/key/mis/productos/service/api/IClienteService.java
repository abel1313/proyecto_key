package com.ventas.key.mis.productos.service.api;

import java.util.List;
import java.util.Optional;

import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.models.ClienteBusquedaDto;
import com.ventas.key.mis.productos.models.ICrud;
import com.ventas.key.mis.productos.models.PageableDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;

public interface IClienteService extends ICrud<
                                                Cliente,
                                                List<Cliente>,
                                                Optional<Cliente>,
                                                Integer,
                                                PginaDto<List<Cliente>>>{

    ResponseGeneric<Optional<Cliente>> findClienteById(int id);

    PageableDto<List<ClienteBusquedaDto>> buscarClientes(String nombre, int page, int size);

}
