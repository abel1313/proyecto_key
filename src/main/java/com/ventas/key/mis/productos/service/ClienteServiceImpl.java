package com.ventas.key.mis.productos.service;

import java.util.List;
import java.util.Optional;

import com.ventas.key.mis.productos.models.ClienteBusquedaDto;
import com.ventas.key.mis.productos.models.PageableDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.api.IClienteService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IClienteRepository;

@Service
public class ClienteServiceImpl extends CrudAbstractServiceImpl<Cliente, List<Cliente>, Optional<Cliente>, Integer, PginaDto<List<Cliente>>>
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
    @Cacheable(value = "clienteCache", key = "#id")
    public ResponseGeneric<Optional<Cliente>> findClienteById(int id) {
        return new ResponseGeneric<>(this.iClienteRepository.findClienteById(id));
    }

    @Override
    @Cacheable(value = "clienteCache", key = "#nombre + ':' + #page + ':' + #size")
    public PageableDto<List<ClienteBusquedaDto>> buscarClientes(String nombre, int page, int size) {
        Page<ClienteBusquedaDto> resultado = iClienteRepository.buscarPorNombre(nombre, PageRequest.of(page, size));
        PageableDto<List<ClienteBusquedaDto>> dto = new PageableDto<>();
        dto.setList(resultado.getContent());
        dto.setTotalPaginas(resultado.getTotalPages());
        return dto;
    }
}
