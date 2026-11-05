package com.ventas.key.mis.productos.service;

import com.ventas.key.hexagonal.busquedapedido.infraestructura.cache.VersionDeListasDePedidos;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final CacheManager cacheManager;
    private final VersionDeListasDePedidos versionDeListas;

    /**
     * Limpia las cachés YA y, si hay una transacción en curso, las vuelve a limpiar DESPUÉS de que se confirme.
     *
     * <p>Por qué dos veces: las ventas, los pedidos y las cancelaciones llaman a este método dentro de su transacción, antes de que la base
     * confirme el cambio. En ese hueco cualquier consulta (la misma pantalla, otra réplica, el aviso por Rabbit) vuelve a llenar la caché con
     * el stock VIEJO y ahí se queda hasta que venza (1 a 6 horas). La segunda limpieza, ya con el cambio confirmado, lo borra.
     */
    public List<String> evictAll() {
        List<String> limpiadas = limpiar();
        evictAllDespuesDelCommit();
        return limpiadas;
    }

    /** Solo registra la limpieza posterior al commit (para los métodos que ya limpian con {@code @CacheEvict}). Sin transacción: no hace nada. */
    public void evictAllDespuesDelCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    limpiar();
                }
            });
        }
    }

    private List<String> limpiar() {
        List<String> limpiadas = new ArrayList<>();
        cacheManager.getCacheNames().forEach(nombre -> {
            Cache cache = cacheManager.getCache(nombre);
            if (cache != null) {
                cache.clear();
                limpiadas.add(nombre);
            }
        });
        // Las listas de Mis pedidos no son una cache de Spring (llevan version, R18 de busquedapedido).
        versionDeListas.subir("limpiar todas las caches");
        log.info("Caches invalidadas: {}", limpiadas);
        return limpiadas;
    }
}
