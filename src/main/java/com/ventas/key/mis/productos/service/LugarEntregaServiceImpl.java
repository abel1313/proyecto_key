package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.config.RabbitMQConfig;
import com.ventas.key.mis.productos.entity.LugarEntrega;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.ILugarEntregaRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class LugarEntregaServiceImpl extends CrudAbstractServiceImpl<
        LugarEntrega,
        List<LugarEntrega>,
        Optional<LugarEntrega>,
        Integer,
        PginaDto<List<LugarEntrega>>> {

    private final ILugarEntregaRepository iLugarEntregaRepository;

    @Autowired private CacheService cacheService;
    @Autowired private RabbitTemplate rabbitTemplate;

    public LugarEntregaServiceImpl(ILugarEntregaRepository repository, ErrorGenerico error) {
        super(repository, error);
        this.iLugarEntregaRepository = repository;
    }

    @Override
    public LugarEntrega save(LugarEntrega req) {
        LugarEntrega resultado = super.save(req);
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
        return resultado;
    }

    // La implementacion base (CrudAbstractServiceImpl.delete) no hace nada -- hay que
    // sobreescribirla para que si borre, igual que VarianteServiceImpl.delete. Los pedidos
    // que ya tenian este lugar quedan con lugar_entrega_id = NULL (FK ON DELETE SET NULL).
    @Transactional
    @Override
    public LugarEntrega delete(Integer id) throws Exception {
        LugarEntrega lugar = iLugarEntregaRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Lugar de entrega no encontrado: " + id));
        iLugarEntregaRepository.delete(lugar);
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
        return lugar;
    }
}
