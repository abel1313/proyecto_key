package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.ClienteSinRegistro;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IClienteRepository;
import com.ventas.key.mis.productos.repository.IClienteSinRegistroRepository;
import com.ventas.key.mis.productos.service.api.IClienteSinRegistro;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ClienteSinRegistroImpl extends CrudAbstractServiceImpl<ClienteSinRegistro, List<ClienteSinRegistro>, Optional<ClienteSinRegistro>, Integer, PginaDto<List<ClienteSinRegistro>>>
        implements IClienteSinRegistro {
    public ClienteSinRegistroImpl(
            final IClienteSinRegistroRepository iRepository,
            final ErrorGenerico eGenerico
    ){
        super(iRepository, eGenerico);
    }
}
