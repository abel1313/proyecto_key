package com.ventas.key.mis.productos.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.entity.GanadorRifa;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IConfigurarRifaRepository;
import com.ventas.key.mis.productos.repository.IGanadorRifaRepository;

@Service
public class GanadorRifaServiceImpl extends CrudAbstractServiceImpl< GanadorRifa,List<GanadorRifa>,Optional<GanadorRifa>, Integer, PginaDto<List<GanadorRifa>>> {

    private final IGanadorRifaRepository iRifaRepository;
    private final ErrorGenerico errorGenerico;
    public GanadorRifaServiceImpl(
        final IGanadorRifaRepository iRifaRepository,
        final ErrorGenerico eGenerico
    ){
        super(iRifaRepository, eGenerico);
        this.iRifaRepository = iRifaRepository;
        this.errorGenerico = eGenerico;
    }

}
