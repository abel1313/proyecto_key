package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.CantidadFlorValida;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.ICantidadFlorValidaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CantidadFlorValidaServiceImpl extends CrudAbstractServiceImpl<
        CantidadFlorValida,
        List<CantidadFlorValida>,
        Optional<CantidadFlorValida>,
        Integer,
        PginaDto<List<CantidadFlorValida>>> {

    private final ICantidadFlorValidaRepository iCantidadFlorValidaRepository;

    public CantidadFlorValidaServiceImpl(ICantidadFlorValidaRepository repository, ErrorGenerico error) {
        super(repository, error);
        this.iCantidadFlorValidaRepository = repository;
    }

    // La implementacion base (CrudAbstractServiceImpl.delete) no hace nada -- hay que
    // sobreescribirla para que si borre, igual que LugarEntregaServiceImpl.delete.
    @Transactional
    @Override
    public CantidadFlorValida delete(Integer id) throws Exception {
        CantidadFlorValida cantidad = iCantidadFlorValidaRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Cantidad valida no encontrada: " + id));
        iCantidadFlorValidaRepository.delete(cantidad);
        return cantidad;
    }
}
