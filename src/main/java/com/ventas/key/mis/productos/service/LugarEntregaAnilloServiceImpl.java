package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.LugarEntrega;
import com.ventas.key.mis.productos.entity.LugarEntregaAnillo;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.AnilloRequest;
import com.ventas.key.mis.productos.models.AnilloResponse;
import com.ventas.key.mis.productos.models.CalcularCostoEnvioResponse;
import com.ventas.key.mis.productos.repository.ILugarEntregaAnilloRepository;
import com.ventas.key.mis.productos.repository.ILugarEntregaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Ver DISENO_ZONAS_POR_ANILLO.md (repo compartido) -- cobro de envio segun en que anillo
// (rango de distancia al centro de la zona) cae el punto exacto marcado en el mapa.
@Service
public class LugarEntregaAnilloServiceImpl {

    private static final double RADIO_TIERRA_METROS = 6371000;

    private final ILugarEntregaAnilloRepository iLugarEntregaAnilloRepository;
    private final ILugarEntregaRepository iLugarEntregaRepository;
    private final ProductoSombraServiceImpl productoSombraService;
    private final CacheService cacheService;

    public LugarEntregaAnilloServiceImpl(ILugarEntregaAnilloRepository iLugarEntregaAnilloRepository,
                                          ILugarEntregaRepository iLugarEntregaRepository,
                                          ProductoSombraServiceImpl productoSombraService,
                                          CacheService cacheService) {
        this.iLugarEntregaAnilloRepository = iLugarEntregaAnilloRepository;
        this.iLugarEntregaRepository = iLugarEntregaRepository;
        this.productoSombraService = productoSombraService;
        this.cacheService = cacheService;
    }

    public List<AnilloResponse> listar(Integer lugarEntregaId) {
        return iLugarEntregaAnilloRepository.findByLugarEntregaIdOrderByRadioMetrosAsc(lugarEntregaId)
                .stream().map(this::aResponse).toList();
    }

    // Usado por FlorPedidoServiceImpl.calcularEnvio para decidir si exigir el punto marcado
    // (lat/lng) antes de poder cobrar, sin duplicar la logica de calcularCosto.
    public boolean tieneAnillos(Integer lugarEntregaId) {
        return !iLugarEntregaAnilloRepository.findByLugarEntregaIdOrderByRadioMetrosAsc(lugarEntregaId).isEmpty();
    }

    @Transactional
    public AnilloResponse crear(Integer lugarEntregaId, AnilloRequest req) {
        LugarEntrega lugar = iLugarEntregaRepository.findById(lugarEntregaId)
                .orElseThrow(() -> new ExceptionDataNotFound("Lugar de entrega no encontrado: " + lugarEntregaId));
        validarRequest(req);

        LugarEntregaAnillo anillo = new LugarEntregaAnillo();
        anillo.setLugarEntrega(lugar);
        anillo.setRadioMetros(req.getRadioMetros());
        anillo.setCostoEnvio(req.getCostoEnvio());
        anillo.setOrden(req.getOrden());
        anillo.setVariante(productoSombraService.crear(
                nombreVariante(lugar, req.getRadioMetros()), req.getCostoEnvio(), 0.0,
                ProductoSombraServiceImpl.STOCK_SIN_CONTROL));

        LugarEntregaAnillo guardado = iLugarEntregaAnilloRepository.save(anillo);
        cacheService.evictAll();
        return aResponse(guardado);
    }

    @Transactional
    public AnilloResponse editar(Integer anilloId, AnilloRequest req) {
        LugarEntregaAnillo anillo = iLugarEntregaAnilloRepository.findById(anilloId)
                .orElseThrow(() -> new ExceptionDataNotFound("Anillo no encontrado: " + anilloId));
        validarRequest(req);

        anillo.setRadioMetros(req.getRadioMetros());
        anillo.setCostoEnvio(req.getCostoEnvio());
        anillo.setOrden(req.getOrden());
        if (anillo.getVariante() != null) {
            productoSombraService.sincronizar(anillo.getVariante(),
                    nombreVariante(anillo.getLugarEntrega(), req.getRadioMetros()), req.getCostoEnvio(), 0.0,
                    ProductoSombraServiceImpl.STOCK_SIN_CONTROL);
        }

        LugarEntregaAnillo guardado = iLugarEntregaAnilloRepository.save(anillo);
        cacheService.evictAll();
        return aResponse(guardado);
    }

    // Igual que LugarEntregaServiceImpl.delete: borra el anillo (config) pero deja la variante
    // sombra huerfana sin tocar, para que los pedidos ya creados con ese anillo sigan
    // referenciando una variante valida.
    @Transactional
    public void eliminar(Integer anilloId) {
        LugarEntregaAnillo anillo = iLugarEntregaAnilloRepository.findById(anilloId)
                .orElseThrow(() -> new ExceptionDataNotFound("Anillo no encontrado: " + anilloId));
        iLugarEntregaAnilloRepository.delete(anillo);
        cacheService.evictAll();
    }

    public CalcularCostoEnvioResponse calcularCosto(Integer lugarEntregaId, Double latitud, Double longitud) {
        LugarEntrega lugar = iLugarEntregaRepository.findById(lugarEntregaId)
                .orElseThrow(() -> new ExceptionDataNotFound("Lugar de entrega no encontrado: " + lugarEntregaId));
        List<LugarEntregaAnillo> anillos =
                iLugarEntregaAnilloRepository.findByLugarEntregaIdOrderByRadioMetrosAsc(lugarEntregaId);

        // Zona sin anillos configurados todavia -- se comporta igual que hoy, costo fijo de la
        // zona completa, sin geocerca.
        if (anillos.isEmpty()) {
            Integer varianteId = lugar.getCostoEnvio() != null && lugar.getCostoEnvio() > 0 && lugar.getVariante() != null
                    ? lugar.getVariante().getId() : null;
            return new CalcularCostoEnvioResponse(true, lugar.getCostoEnvio(), null, varianteId);
        }

        if (lugar.getLatitud() == null || lugar.getLongitud() == null) {
            throw new RuntimeException("La zona '" + lugar.getNombre() + "' tiene anillos configurados pero no "
                    + "tiene centro (latitud/longitud) definido -- no se puede calcular la distancia.");
        }
        if (latitud == null || longitud == null) {
            throw new RuntimeException("Falta el punto marcado en el mapa (latitud/longitud) para calcular el costo.");
        }

        double distancia = distanciaMetros(lugar.getLatitud(), lugar.getLongitud(), latitud, longitud);

        // anillos viene ordenado por radioMetros ascendente -- el primero cuyo radio alcance la
        // distancia es el mas especifico que contiene al punto.
        for (LugarEntregaAnillo anillo : anillos) {
            if (distancia <= anillo.getRadioMetros()) {
                Integer varianteId = anillo.getVariante() != null ? anillo.getVariante().getId() : null;
                return new CalcularCostoEnvioResponse(true, anillo.getCostoEnvio(), anillo.getId(), varianteId);
            }
        }
        return new CalcularCostoEnvioResponse(false, null, null, null);
    }

    private void validarRequest(AnilloRequest req) {
        if (req.getRadioMetros() == null || req.getRadioMetros() <= 0) {
            throw new RuntimeException("El radio del anillo debe ser mayor a 0 metros.");
        }
        if (req.getCostoEnvio() == null || req.getCostoEnvio() < 0) {
            throw new RuntimeException("El costo de envio del anillo es obligatorio y no puede ser negativo.");
        }
    }

    private String nombreVariante(LugarEntrega lugar, double radioMetros) {
        return "Envio a " + lugar.getNombre() + " (hasta " + Math.round(radioMetros) + "m)";
    }

    private AnilloResponse aResponse(LugarEntregaAnillo a) {
        return new AnilloResponse(a.getId(), a.getLugarEntrega().getId(), a.getRadioMetros(), a.getCostoEnvio(), a.getOrden());
    }

    // Formula haversine -- distancia en metros entre dos puntos lat/lng.
    private static double distanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RADIO_TIERRA_METROS * c;
    }
}
