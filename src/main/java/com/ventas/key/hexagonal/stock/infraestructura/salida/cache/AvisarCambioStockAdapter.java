package com.ventas.key.hexagonal.stock.infraestructura.salida.cache;

import com.ventas.key.hexagonal.stock.dominio.puerto.salida.AvisarCambioStockPort;
import com.ventas.key.mis.productos.config.RabbitMQConfig;
import com.ventas.key.mis.productos.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks &amp; Drivers]
 *
 * <p>Lo mismo que hacen los demas guardados de modelos: limpia las caches de Redis y avisa al micro
 * de imagenes por Rabbit. Si Rabbit no responde no se deshace el ajuste (igual que en
 * ProductosServiceImpl): la cache de Redis ya quedo limpia.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AvisarCambioStockAdapter implements AvisarCambioStockPort {

    private final CacheService cacheService;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void stockCambio(Integer productoId) {
        cacheService.evictAll();
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
        } catch (Exception e) {
            log.warn("No se pudo avisar a Rabbit del cambio de stock del modelo {} (no bloquea): {}", productoId, e.getMessage());
        }
    }
}
