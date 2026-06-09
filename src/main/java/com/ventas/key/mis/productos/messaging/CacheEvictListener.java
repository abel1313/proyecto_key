package com.ventas.key.mis.productos.messaging;

import com.ventas.key.mis.productos.config.RabbitMQConfig;
import com.ventas.key.mis.productos.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheEvictListener {

    private final CacheService cacheService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_CACHE_EVICT_ALL)
    public void evictAll(Object message) {
        cacheService.evictAll();
    }
}
