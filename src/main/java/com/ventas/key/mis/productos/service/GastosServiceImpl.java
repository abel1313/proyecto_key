package com.ventas.key.mis.productos.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ventas.key.mis.productos.entity.Gastos;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.repository.BaseRepository;
import com.ventas.key.mis.productos.repository.IGastosRepository;
import com.ventas.key.mis.productos.service.api.IGastosService;

@Service
public class GastosServiceImpl extends CrudAbstract<Gastos,
                                                Gastos,
                                                List<Gastos>, 
                                                Optional<Gastos>, 
                                                Integer> implements IGastosService{

    private final IGastosRepository iRepository;
    private final ErrorGenerico eGenerico;
    public GastosServiceImpl(final IGastosRepository iRepository, ErrorGenerico error) {
        super(iRepository, error);
        this.iRepository = iRepository;
        this.eGenerico = error;
    }

    public Gastos saveGastosNT(Gastos gastos){
        return this.iRepository.save(gastos);
    }


}
