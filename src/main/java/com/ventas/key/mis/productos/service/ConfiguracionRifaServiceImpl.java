package com.ventas.key.mis.productos.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IClienteRepository;
import com.ventas.key.mis.productos.repository.IConfigurarRifaRepository;

@Service
public class ConfiguracionRifaServiceImpl extends CrudAbstractServiceImpl< ConfigurarRifa,List<ConfigurarRifa>,Optional<ConfigurarRifa>, Integer, PginaDto<List<ConfigurarRifa>>> {

    private final IConfigurarRifaRepository iRifaRepository;
    private final ErrorGenerico errorGenerico;
    public ConfiguracionRifaServiceImpl(
        final IConfigurarRifaRepository iRifaRepository,
        final ErrorGenerico eGenerico
    ){
        super(iRifaRepository, eGenerico);
        this.iRifaRepository = iRifaRepository;
        this.errorGenerico = eGenerico;
    }

}
