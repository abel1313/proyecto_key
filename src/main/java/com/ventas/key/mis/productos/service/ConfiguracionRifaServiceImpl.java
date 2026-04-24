package com.ventas.key.mis.productos.service;

import java.util.List;
import java.util.Optional;

import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
import org.springframework.stereotype.Service;

import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IConfigurarRifaRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;

@Service
public class ConfiguracionRifaServiceImpl extends CrudAbstractServiceImpl<ConfigurarRifa, List<ConfigurarRifa>, Optional<ConfigurarRifa>, Integer, PginaDto<List<ConfigurarRifa>>> {

    private final IConfigurarRifaRepository iRifaRepository;
    private final IProductosRepository iProductosRepository;

    public ConfiguracionRifaServiceImpl(
        final IConfigurarRifaRepository iRifaRepository,
        final IProductosRepository iProductosRepository,
        final ErrorGenerico eGenerico
    ){
        super(iRifaRepository, eGenerico);
        this.iRifaRepository = iRifaRepository;
        this.iProductosRepository = iProductosRepository;
    }

    public List<ConfigurarRifa> buscarActivas() {
        return iRifaRepository.findByActivaTrue();
    }

    @Override
    public ConfigurarRifa save(ConfigurarRifa req) throws ExceptionErrorInesperado {
        if (req.getProducto() == null || req.getProducto().getId() == null) {
            throw new ExceptionErrorInesperado("Debe indicar el producto de la rifa");
        }
        Producto producto = iProductosRepository.findById(req.getProducto().getId())
                .orElseThrow(() -> new ExceptionErrorInesperado("Producto no encontrado"));
        req.setProducto(producto);
        return super.save(req);
    }

}
