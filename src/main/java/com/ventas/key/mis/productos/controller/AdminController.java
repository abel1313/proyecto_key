package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.models.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final CacheManager cacheManager;

    @DeleteMapping("/cache")
    public ResponseEntity<ResponseGeneric<List<String>>> limpiarCache() {
        List<String> limpiadas = new ArrayList<>();
        cacheManager.getCacheNames().forEach(nombre -> {
            Cache cache = cacheManager.getCache(nombre);
            if (cache != null) {
                cache.clear();
                limpiadas.add(nombre);
            }
        });
        log.info("Cache Redis limpiada. Caches afectadas: {}", limpiadas);
        return ResponseEntity.ok(new ResponseGeneric<List<String>>(limpiadas));
    }
}