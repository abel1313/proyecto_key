package com.ventas.key.mis.productos.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.ICodigoBarrasRepository;
import com.ventas.key.mis.productos.repository.IConfigurarRifaRepository;
import com.ventas.key.mis.productos.service.api.ICodigoBarrasService;

@Service
public class CodigoBarraImpl extends CrudAbstractServiceImpl< CodigoBarra,List<CodigoBarra>,Optional<CodigoBarra>, Integer, PginaDto<List<CodigoBarra>>> 
implements ICodigoBarrasService{

    private final ICodigoBarrasRepository iRifaRepository;
    private final ErrorGenerico errorGenerico;
    public CodigoBarraImpl(
        final ICodigoBarrasRepository iRifaRepository,
        final ErrorGenerico eGenerico
    ){
        super(iRifaRepository, eGenerico);
        this.iRifaRepository = iRifaRepository;
        this.errorGenerico = eGenerico;
    }
    @Override
    public Optional<CodigoBarra> findByCodigoBarra(String codigoBarra) {
        return iRifaRepository.findByCodigoBarras(codigoBarra);
    }

}
