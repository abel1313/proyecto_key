package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.config.RabbitMQConfig;
import com.ventas.key.mis.productos.entity.LugarEntrega;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
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
    private final ProductoSombraServiceImpl productoSombraService;

    @Autowired private CacheService cacheService;
    @Autowired private RabbitTemplate rabbitTemplate;

    public LugarEntregaServiceImpl(ILugarEntregaRepository repository, ErrorGenerico error,
                                    ProductoSombraServiceImpl productoSombraService) {
        super(repository, error);
        this.iLugarEntregaRepository = repository;
        this.productoSombraService = productoSombraService;
    }

    // La variante "sombra" solo se crea/sincroniza si costoEnvio viene con valor -- sin costo
    // no hace falta una linea de cobro por envio (ver ProductoSombraServiceImpl y FlorPedido).
    @Override
    public LugarEntrega save(LugarEntrega req) {
        if (req.getCostoEnvio() != null) {
            int stock = ProductoSombraServiceImpl.STOCK_SIN_CONTROL;
            Variantes varianteExistente = req.getId() != null
                    ? iLugarEntregaRepository.findById(req.getId()).map(LugarEntrega::getVariante).orElse(null)
                    : null;
            if (varianteExistente != null) {
                productoSombraService.sincronizar(varianteExistente, "Envio a " + req.getNombre(), req.getCostoEnvio(), 0.0, stock);
                req.setVariante(varianteExistente);
            } else {
                req.setVariante(productoSombraService.crear("Envio a " + req.getNombre(), req.getCostoEnvio(), 0.0, stock));
            }
        }
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
