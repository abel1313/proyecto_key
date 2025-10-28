package com.ventas.key.mis.productos.service.api;

import java.util.List;
import java.util.Optional;

import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.models.ICrud;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import org.springframework.data.repository.query.Param;

public interface IClienteService extends ICrud<
                                                Cliente,
                                                List<Cliente>, 
                                                Optional<Cliente>, 
                                                Integer,
                                                PginaDto<List<Cliente>>>{

    ResponseGeneric<Optional<Cliente>> findClienteById(int id);

}
