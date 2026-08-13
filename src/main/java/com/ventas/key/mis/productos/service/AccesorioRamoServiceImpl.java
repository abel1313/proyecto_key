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
        if (Boolean.TRUE.equals(req.getEsPapel()) && req.getFloresPorPliego() != null && req.getFloresPorPliego() <= 0) {
            throw new RuntimeException("Flores por pliego debe ser mayor a cero");
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

    // Regla del umbral, centralizada aqui porque la usan tanto RamoArmadoServiceImpl como
    // FlorPedidoServiceImpl: el accesorio activo marcado "es papel" se agrega solo cuando
    // cantidadFinal supera su propio umbralActivacion (configurable por el admin, ver
    // AccesorioRamo.umbralActivacion). Sin accesorio "es papel" activo, o con umbralActivacion
    // null, la regla nunca se dispara sola -- queda como accesorio opcional normal.
    public Optional<AccesorioRamo> obtenerPapelAutomaticoSiAplica(int cantidadFinal) {
        return iAccesorioRamoRepository.findFirstByEsPapelTrueAndActivoTrue()
                .filter(papel -> papel.getUmbralActivacion() != null && cantidadFinal > papel.getUmbralActivacion());
    }

    // Cuantos pliegos hace falta para envolver "cantidadFlores" flores. Un pliego empezado se
    // gasta completo (redondeo hacia arriba). Null si el accesorio no tiene floresPorPliego
    // configurado -- caso retrocompatible, precio fijo unico (ver calcularPrecioPapel).
    public Integer calcularPliegosPapel(AccesorioRamo papel, int cantidadFlores) {
        if (papel.getFloresPorPliego() == null || papel.getFloresPorPliego() <= 0) {
            return null;
        }
        return (int) Math.ceil((double) cantidadFlores / papel.getFloresPorPliego());
    }

    // Precio real del papel para un ramo de "cantidadFlores" flores: pliegosNecesarios x precio
    // (precio se interpreta como "precio por pliego" en cuanto floresPorPliego esta configurado).
    // Sin floresPorPliego configurado, se mantiene el precio fijo unico de antes.
    public double calcularPrecioPapel(AccesorioRamo papel, int cantidadFlores) {
        Integer pliegos = calcularPliegosPapel(papel, cantidadFlores);
        return pliegos != null ? pliegos * papel.getPrecio() : papel.getPrecio();
    }
}
