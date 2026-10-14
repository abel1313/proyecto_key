package com.ventas.key.hexagonal.busquedapedido.infraestructura.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 *
 * <p>R18: un numero en Redis que sube cada vez que cambia algo que se ve en una tarjeta de Mis pedidos
 * (pedido, articulos del pedido, abono, grupo, cliente, lugar de entrega, nombre del modelo). Va dentro
 * de la llave del cache de la lista: al subir, todas las listas guardadas dejan de usarse de golpe, sin
 * borrar nada a mano y sin adivinar cual lista toca (un abono puede mover un pedido de Ir pagando a
 * Pagado, y la busqueda por texto cruza todas las formas de cobro).
 *
 * <p>Si Redis no responde, {@link #actual()} regresa null y la lista se calcula sin cache: nunca se
 * frena una venta ni se muestra algo viejo por eso.
 */
@Slf4j
@Component
public class VersionDeListasDePedidos {

    static final String CLAVE = "pedidos:lista:version";

    private final ObjectProvider<StringRedisTemplate> redis;
    private final boolean habilitado;

    public VersionDeListasDePedidos(ObjectProvider<StringRedisTemplate> redis,
                                    @Value("${pedidos.lista.cache.habilitado:true}") boolean habilitado) {
        this.redis = redis;
        this.habilitado = habilitado;
    }

    public boolean habilitado() {
        return habilitado;
    }

    /** La version vigente, o null si no se debe usar cache (apagado o Redis no responde). */
    public String actual() {
        if (!habilitado) {
            return null;
        }
        try {
            StringRedisTemplate r = redis.getIfAvailable();
            if (r == null) {
                return null;
            }
            String v = r.opsForValue().get(CLAVE);
            return v == null ? "0" : v;
        } catch (RuntimeException e) {
            log.warn("Cache de Mis pedidos: no se pudo leer la version en Redis ({}); se calcula sin cache", e.getMessage());
            return null;
        }
    }

    /** Cambio algo que se ve en la lista: las listas guardadas dejan de valer. */
    public void subir(String motivo) {
        if (!habilitado) {
            return;
        }
        try {
            StringRedisTemplate r = redis.getIfAvailable();
            if (r != null) {
                r.opsForValue().increment(CLAVE);
                log.debug("Cache de Mis pedidos: version nueva ({})", motivo);
            }
        } catch (RuntimeException e) {
            log.warn("Cache de Mis pedidos: no se pudo subir la version ({}): {}", motivo, e.getMessage());
        }
    }

    /**
     * Para una escritura que no pasa por JPA (SQL directo): sube la version cuando la transaccion
     * se confirma, antes de que la pantalla reciba la respuesta. Sin transaccion, de inmediato.
     */
    public void subirAlConfirmar(String motivo) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    subir(motivo);
                }
            });
        } else {
            subir(motivo);
        }
    }
}
