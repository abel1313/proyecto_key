package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.config.RabbitMQConfig;
import com.ventas.key.mis.productos.entity.CintaPromocion;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.ICintaPromocionRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CintaPromocionServiceImpl extends CrudAbstractServiceImpl<
        CintaPromocion,
        List<CintaPromocion>,
        Optional<CintaPromocion>,
        Integer,
        PginaDto<List<CintaPromocion>>> {

    private final ICintaPromocionRepository iCintaPromocionRepository;

    @Autowired private CacheService cacheService;
    @Autowired private RabbitTemplate rabbitTemplate;

    public CintaPromocionServiceImpl(ICintaPromocionRepository repository, ErrorGenerico error) {
        super(repository, error);
        this.iCintaPromocionRepository = repository;
    }

    // Publico, sin auth (lo pinta la cinta al visitante) -- cacheado porque se pide en cada
    // carga de pantalla; se invalida en save/delete via cacheService.evictAll().
    @Cacheable(value = "cintaActivosCache")
    public List<CintaPromocion> obtenerActivos() {
        return iCintaPromocionRepository.findByActivoTrueOrderByOrdenAsc();
    }

    @Override
    public CintaPromocion save(CintaPromocion req) {
        CintaPromocion resultado = super.save(req);
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
        return resultado;
    }

    // La implementacion base (CrudAbstractServiceImpl.delete) no hace nada -- hay que
    // sobreescribirla para que si borre, igual que LugarEntregaServiceImpl.delete.
    @Transactional
    @Override
    public CintaPromocion delete(Integer id) throws Exception {
        CintaPromocion cinta = iCintaPromocionRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Frase de cinta no encontrada: " + id));
        iCintaPromocionRepository.delete(cinta);
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
        return cinta;
    }
}
