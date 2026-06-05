package com.ventas.key.mis.productos.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final CacheManager cacheManager;

    public List<String> evictAll() {
        List<String> limpiadas = new ArrayList<>();
        cacheManager.getCacheNames().forEach(nombre -> {
            Cache cache = cacheManager.getCache(nombre);
            if (cache != null) {
                cache.clear();
                limpiadas.add(nombre);
            }
        });
        log.info("Caches invalidadas: {}", limpiadas);
        return limpiadas;
    }
}
