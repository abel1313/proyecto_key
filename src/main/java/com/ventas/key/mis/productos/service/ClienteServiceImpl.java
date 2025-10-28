package com.ventas.key.mis.productos.service;

import java.util.List;
import java.util.Optional;

import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.api.IClienteService;
import org.springframework.stereotype.Service;

import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.entity.Venta;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IClienteRepository;

import lombok.RequiredArgsConstructor;

@Service
public class ClienteServiceImpl extends CrudAbstractServiceImpl< Cliente,List<Cliente>,Optional<Cliente>, Integer, PginaDto<List<Cliente>>>
implements IClienteService {

    private final IClienteRepository iClienteRepository;
    private final ErrorGenerico errorGenerico;
    public ClienteServiceImpl(
        final IClienteRepository iRepository,
        final ErrorGenerico eGenerico
    ){
        super(iRepository, eGenerico);
        this.iClienteRepository = iRepository;
        this.errorGenerico = eGenerico;
    }


    @Override
    public ResponseGeneric<Optional<Cliente>> findClienteById(int id) {
        return new ResponseGeneric<>(this.iClienteRepository.findClienteById(id));
    }
}
