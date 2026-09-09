package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.TipoFlor;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.ITipoFlorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// TipoFlor es la "especie" (ej. "Rosa eterna") -- no vende directo, no tiene variante sombra
// propia. Lo vendible son sus colores (ver ColorFlor/ColorFlorServiceImpl).
@Service
public class TipoFlorServiceImpl extends CrudAbstractServiceImpl<
        TipoFlor,
        List<TipoFlor>,
        Optional<TipoFlor>,
        Integer,
        PginaDto<List<TipoFlor>>> {

    private final ITipoFlorRepository iTipoFlorRepository;

    public TipoFlorServiceImpl(ITipoFlorRepository repository, ErrorGenerico error) {
        super(repository, error);
        this.iTipoFlorRepository = repository;
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
