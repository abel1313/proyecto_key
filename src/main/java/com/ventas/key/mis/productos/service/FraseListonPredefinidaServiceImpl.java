package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.FraseListonPredefinida;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IFraseListonPredefinidaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class FraseListonPredefinidaServiceImpl extends CrudAbstractServiceImpl<
        FraseListonPredefinida,
        List<FraseListonPredefinida>,
        Optional<FraseListonPredefinida>,
        Integer,
        PginaDto<List<FraseListonPredefinida>>> {

    private final IFraseListonPredefinidaRepository iFraseListonPredefinidaRepository;
    private final ProductoSombraServiceImpl productoSombraService;

    public FraseListonPredefinidaServiceImpl(IFraseListonPredefinidaRepository repository, ErrorGenerico error,
                                              ProductoSombraServiceImpl productoSombraService) {
        super(repository, error);
        this.iFraseListonPredefinidaRepository = repository;
        this.productoSombraService = productoSombraService;
    }

    // Antes de guardar, crea (alta) o sincroniza (edicion) la variante "sombra" -- ver
    // ProductoSombraServiceImpl. Stock fijo alto: una frase no tiene limite de "existencias".
    @Transactional
    @Override
    public FraseListonPredefinida save(FraseListonPredefinida req) {
        int stock = ProductoSombraServiceImpl.STOCK_SIN_CONTROL;
        if (req.getId() != null) {
            FraseListonPredefinida existente = iFraseListonPredefinidaRepository.findById(req.getId())
                    .orElseThrow(() -> new ExceptionDataNotFound("Frase no encontrada: " + req.getId()));
            Variantes variante = existente.getVariante();
            productoSombraService.sincronizar(variante, req.getTexto(), req.getPrecio(), 0.0, stock);
            req.setVariante(variante);
        } else {
            Variantes variante = productoSombraService.crear(req.getTexto(), req.getPrecio(), 0.0, stock);
            req.setVariante(variante);
        }
        return super.save(req);
    }

    // La implementacion base (CrudAbstractServiceImpl.delete) no hace nada -- hay que
    // sobreescribirla para que si borre, igual que LugarEntregaServiceImpl.delete.
    @Transactional
    @Override
    public FraseListonPredefinida delete(Integer id) throws Exception {
        FraseListonPredefinida frase = iFraseListonPredefinidaRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Frase no encontrada: " + id));
        iFraseListonPredefinidaRepository.delete(frase);
        return frase;
    }
}
