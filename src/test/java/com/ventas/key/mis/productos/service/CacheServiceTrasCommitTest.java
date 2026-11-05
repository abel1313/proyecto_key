package com.ventas.key.mis.productos.service;

import com.ventas.key.hexagonal.busquedapedido.infraestructura.cache.VersionDeListasDePedidos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Revisión 2026-11-05 (reporte del dueño: tras vender o cancelar, la búsqueda seguía mostrando el stock viejo hasta recargar).
 * Las ventas limpian las cachés DENTRO de su transacción, antes del commit: una consulta en ese hueco las vuelve a llenar con el
 * stock viejo. La segunda limpieza, tras el commit, lo corrige.
 */
class CacheServiceTrasCommitTest {

    private final ConcurrentMapCacheManager caches = new ConcurrentMapCacheManager("obtenerProductosCache");
    private final CacheService servicio = new CacheService(caches, mock(VersionDeListasDePedidos.class));

    @AfterEach
    void limpiarHilo() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    @DisplayName("una consulta entre la limpieza y el commit vuelve a llenar la caché con el stock viejo; el commit la limpia otra vez")
    void despuesDelCommitSeVuelveALimpiar() {
        TransactionSynchronizationManager.initSynchronization();
        caches.getCache("obtenerProductosCache").put("pagina1", "stock=5");

        servicio.evictAll();                                              // dentro de la transacción, antes del commit
        assertThat(caches.getCache("obtenerProductosCache").get("pagina1")).isNull();

        caches.getCache("obtenerProductosCache").put("pagina1", "stock=5");   // otra consulta la rellena con el valor VIEJO
        assertThat(caches.getCache("obtenerProductosCache").get("pagina1")).isNotNull();

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);   // el commit
        assertThat(caches.getCache("obtenerProductosCache").get("pagina1")).as("tras el commit ya no queda el valor viejo").isNull();
    }

    @Test
    @DisplayName("sin transacción limpia una sola vez y no falla")
    void sinTransaccion() {
        caches.getCache("obtenerProductosCache").put("k", "v");
        assertThat(servicio.evictAll()).contains("obtenerProductosCache");
        assertThat(caches.getCache("obtenerProductosCache").get("k")).isNull();
    }
}
