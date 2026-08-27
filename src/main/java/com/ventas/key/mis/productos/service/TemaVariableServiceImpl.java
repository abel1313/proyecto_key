package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.TemaVariable;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionOperacionNoPermitida;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.ITemaVariableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TemaVariableServiceImpl extends CrudAbstractServiceImpl<
        TemaVariable,
        List<TemaVariable>,
        Optional<TemaVariable>,
        Integer,
        PginaDto<List<TemaVariable>>> {

    private final ITemaVariableRepository repository;

    public TemaVariableServiceImpl(ITemaVariableRepository repository, ErrorGenerico error) {
        super(repository, error);
        this.repository = repository;
    }

    /** GET /v1/tema-variable/activo (público) -- todo el catálogo, ordenado para pintar la pantalla. */
    public List<TemaVariable> activas() {
        return repository.findAllOrdenadas();
    }

    @Override
    public TemaVariable save(TemaVariable entity) throws ExceptionDataNotFound {
        validarClaveUnica(entity.getClave(), entity.getId());
        return super.save(entity);
    }

    // La implementacion base (CrudAbstractServiceImpl.delete) no hace nada -- hay que
    // sobreescribirla para que si borre (mismo gotcha que LugarEntregaServiceImpl.delete).
    // A diferencia de Menu/Submenu, borrar una variable es seguro: el front simplemente deja de
    // aplicar ese override y el .scss vuelve a su valor fijo de código -- por eso no hace falta
    // ninguna protección de "no dejar la última".
    @Transactional
    @Override
    public TemaVariable delete(Integer id) throws Exception {
        TemaVariable variable = repository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Variable de tema no encontrada: " + id));
        repository.delete(variable);
        return variable;
    }

    private void validarClaveUnica(String clave, Integer idPropio) {
        if (clave == null || clave.isBlank()) return;
        boolean choca = repository.findByClaveIgnoreCase(clave.trim()).stream()
                .anyMatch(v -> !v.getId().equals(idPropio));
        if (choca) {
            throw new ExceptionOperacionNoPermitida("Ya existe una variable con la clave \"" + clave + "\".");
        }
    }
}
