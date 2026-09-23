package com.ventas.key.hexagonal.precio.infraestructura.salida.cache;

import com.ventas.key.hexagonal.precio.dominio.puerto.salida.AvisarCambioCatalogoPort;
import com.ventas.key.mis.productos.config.RabbitMQConfig;
import com.ventas.key.mis.productos.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks &amp; Drivers]
 *
 * <p>Sin esto la card y el cobro seguirian viendo el precio viejo hasta que expire la cache.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogoCacheAdapter implements AvisarCambioCatalogoPort {

    private final CacheService cacheService;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void catalogoCambio() {
        cacheService.evictAll();
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES,
                    RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
        } catch (Exception e) {
            log.warn("No se pudo avisar a Rabbit para invalidar cache (no bloquea el cambio de precio): {}", e.getMessage());
        }
    }
}
