package com.ventas.key.mis.productos.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.entity.Venta;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IClienteRepository;

import lombok.RequiredArgsConstructor;

@Service
public class ClienteServiceImpl extends CrudAbstractServiceImpl< Cliente,List<Cliente>,Optional<Cliente>, Integer, PginaDto<List<Cliente>>> {

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


}
