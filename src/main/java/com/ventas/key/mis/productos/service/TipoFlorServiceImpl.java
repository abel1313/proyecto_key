package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.TipoFlor;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.ITipoFlorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TipoFlorServiceImpl extends CrudAbstractServiceImpl<
        TipoFlor,
        List<TipoFlor>,
        Optional<TipoFlor>,
        Integer,
        PginaDto<List<TipoFlor>>> {

    private final ITipoFlorRepository iTipoFlorRepository;
    private final ProductoSombraServiceImpl productoSombraService;

    public TipoFlorServiceImpl(ITipoFlorRepository repository, ErrorGenerico error,
                                ProductoSombraServiceImpl productoSombraService) {
        super(repository, error);
        this.iTipoFlorRepository = repository;
        this.productoSombraService = productoSombraService;
    }

    // Antes de guardar, crea (alta) o sincroniza (edicion) la variante "sombra" que representa
    // este tipo de flor en el catalogo general -- ver ProductoSombraServiceImpl.
    @Transactional
    @Override
    public TipoFlor save(TipoFlor req) {
        int stock = req.getStock() != null ? req.getStock() : 0;
        if (req.getId() != null) {
            TipoFlor existente = iTipoFlorRepository.findById(req.getId())
                    .orElseThrow(() -> new ExceptionDataNotFound("Tipo de flor no encontrado: " + req.getId()));
            Variantes variante = existente.getVariante();
            productoSombraService.sincronizar(variante, req.getNombre(), req.getPrecioPorFlor(), req.getPrecioCosto(), stock);
            req.setVariante(variante);
        } else {
            Variantes variante = productoSombraService.crear(req.getNombre(), req.getPrecioPorFlor(), req.getPrecioCosto(), stock);
            req.setVariante(variante);
        }
        return super.save(req);
    }

    // La implementacion base (CrudAbstractServiceImpl.delete) no hace nada -- hay que
    // sobreescribirla para que si borre, igual que LugarEntregaServiceImpl.delete.
    @Transactional
    @Override
    public TipoFlor delete(Integer id) throws Exception {
        TipoFlor tipoFlor = iTipoFlorRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Tipo de flor no encontrado: " + id));
        iTipoFlorRepository.delete(tipoFlor);
        return tipoFlor;
    }
}
