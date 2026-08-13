package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.AccesorioRamo;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IAccesorioRamoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AccesorioRamoServiceImpl extends CrudAbstractServiceImpl<
        AccesorioRamo,
        List<AccesorioRamo>,
        Optional<AccesorioRamo>,
        Integer,
        PginaDto<List<AccesorioRamo>>> {

    private final IAccesorioRamoRepository iAccesorioRamoRepository;
    private final ProductoSombraServiceImpl productoSombraService;

    public AccesorioRamoServiceImpl(IAccesorioRamoRepository repository, ErrorGenerico error,
                                     ProductoSombraServiceImpl productoSombraService) {
        super(repository, error);
        this.iAccesorioRamoRepository = repository;
        this.productoSombraService = productoSombraService;
    }

    // Antes de guardar, crea (alta) o sincroniza (edicion) la variante "sombra" -- ver
    // ProductoSombraServiceImpl. Stock fijo alto: los accesorios no controlan inventario real.
    @Transactional
    @Override
    public AccesorioRamo save(AccesorioRamo req) {
        // Regla del umbral (ver FlorPedidoServiceImpl): solo puede haber un accesorio activo
        // marcado "es papel" a la vez, o el calculo elegiria uno arbitrario entre varios.
        if (Boolean.TRUE.equals(req.getEsPapel()) && Boolean.TRUE.equals(req.getActivo())) {
            Integer idActual = req.getId() != null ? req.getId() : -1;
            iAccesorioRamoRepository.findFirstByEsPapelTrueAndActivoTrueAndIdNot(idActual)
                    .ifPresent(otro -> {
                        throw new RuntimeException("Ya existe otro accesorio activo marcado como \"es papel\": '"
                                + otro.getNombre() + "'. Desactivalo antes de activar este.");
                    });
        }
        int stock = ProductoSombraServiceImpl.STOCK_SIN_CONTROL;
        if (req.getId() != null) {
            AccesorioRamo existente = iAccesorioRamoRepository.findById(req.getId())
                    .orElseThrow(() -> new ExceptionDataNotFound("Accesorio no encontrado: " + req.getId()));
            Variantes variante = existente.getVariante();
            productoSombraService.sincronizar(variante, req.getNombre(), req.getPrecio(), req.getPrecioCosto(), stock);
            req.setVariante(variante);
        } else {
            Variantes variante = productoSombraService.crear(req.getNombre(), req.getPrecio(), req.getPrecioCosto(), stock);
            req.setVariante(variante);
        }
        return super.save(req);
    }

    // La implementacion base (CrudAbstractServiceImpl.delete) no hace nada -- hay que
    // sobreescribirla para que si borre, igual que LugarEntregaServiceImpl.delete.
    @Transactional
    @Override
    public AccesorioRamo delete(Integer id) throws Exception {
        AccesorioRamo accesorio = iAccesorioRamoRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Accesorio no encontrado: " + id));
        iAccesorioRamoRepository.delete(accesorio);
        return accesorio;
    }
}
