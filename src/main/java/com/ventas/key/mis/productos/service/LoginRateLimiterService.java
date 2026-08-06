package com.ventas.key.mis.productos.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limit de los endpoints de autenticacion — maximo 5 intentos por clave cada 15 minutos.
 *
 * <p>Hallazgo 7 (SEGURIDAD_AUTH.md): el mapa de buckets no tenia ni cota ni limpieza, asi que
 * crecia una entrada por cada IP y por cada username probado y nunca se vaciaba — un atacante
 * mandando usernames aleatorios agotaba la memoria del proceso. Ahora las entradas caducan por
 * inactividad y el tamaño esta acotado.
 *
 * <p>Hallazgo 8: se separo "mirar si quedan intentos" de "gastar un intento", para poder gastar
 * solo cuando la autenticacion falla de verdad (ver {@link #hayIntentosDisponibles} y
 * {@link #registrarFallo}).
 *
 * <p><b>Sigue siendo local al proceso:</b> con N pods el limite efectivo es 5xN. Migrarlo a Redis
 * (que ya esta en el proyecto) es lo que corresponde si algun dia se escala horizontalmente.
 */
@Service
@Slf4j
public class LoginRateLimiterService {

    private static final int MAX_INTENTOS = 5;
    private static final Duration VENTANA = Duration.ofMinutes(15);

    /**
     * Cota dura del mapa. Al alcanzarla se limpia primero; si aun asi sigue lleno se rechaza el
     * intento (fail-closed). Es deliberado: bajo un ataque que genere decenas de miles de claves
     * distintas es preferible rechazar logins que quedarse sin memoria y tirar el micro entero.
     */
    private static final int MAX_ENTRADAS = 50_000;

    /** Bucket + marca de ultimo uso, que es lo que permite caducar las entradas inactivas. */
    private static final class Entrada {
        private final Bucket bucket;
        private volatile long ultimoAccesoMillis;

        private Entrada(Bucket bucket) {
            this.bucket = bucket;
            this.ultimoAccesoMillis = System.currentTimeMillis();
        }
    }

    private final Map<String, Entrada> buckets = new ConcurrentHashMap<>();

    private Bucket crearBucket() {
        Bandwidth limite = Bandwidth.builder()
                .capacity(MAX_INTENTOS)
                .refillIntervally(MAX_INTENTOS, VENTANA)
                .build();
        return Bucket.builder().addLimit(limite).build();
    }

    /**
     * Gasta 1 intento de la clave dada.
     * @return true si aun quedaban intentos, false si se excedio el limite (o el mapa esta saturado).
     */
    public boolean tryConsume(String clave) {
        Entrada entrada = obtener(clave);
        if (entrada == null) {
            return false;
        }
        entrada.ultimoAccesoMillis = System.currentTimeMillis();
        return entrada.bucket.tryConsume(1);
    }

    /**
     * Consulta si quedan intentos <b>sin gastar ninguno</b>. Se usa antes de autenticar, para que
     * un login correcto no consuma cupo: antes, cinco entradas legitimas en 15 minutos
     * autobloqueaban al propio usuario.
     */
    public boolean hayIntentosDisponibles(String clave) {
        Entrada entrada = buckets.get(clave);
        if (entrada == null) {
            return true;
        }
        entrada.ultimoAccesoMillis = System.currentTimeMillis();
        return entrada.bucket.getAvailableTokens() >= 1;
    }

    /** Gasta un intento — se llama solo cuando la autenticacion fallo. */
    public void registrarFallo(String clave) {
        Entrada entrada = obtener(clave);
        if (entrada != null) {
            entrada.ultimoAccesoMillis = System.currentTimeMillis();
            entrada.bucket.tryConsume(1);
        }
    }

    /** Login correcto: se borra el historial de fallos de esa clave. */
    public void limpiarIntentos(String clave) {
        buckets.remove(clave);
    }

    /**
     * Segundos restantes hasta que se recarguen los intentos. No crea entrada: consultar por una
     * clave desconocida no debe hacer crecer el mapa.
     */
    public long segundosHastaRecarga(String clave) {
        Entrada entrada = buckets.get(clave);
        if (entrada == null) {
            return 0;
        }
        return entrada.bucket.getAvailableTokens() == 0 ? VENTANA.getSeconds() : 0;
    }

    private Entrada obtener(String clave) {
        Entrada existente = buckets.get(clave);
        if (existente != null) {
            return existente;
        }
        if (buckets.size() >= MAX_ENTRADAS) {
            limpiarInactivos();
            if (buckets.size() >= MAX_ENTRADAS) {
                log.error("Rate limiter saturado ({} entradas): se rechaza el intento por precaucion", buckets.size());
                return null;
            }
        }
        return buckets.computeIfAbsent(clave, k -> new Entrada(crearBucket()));
    }

    /**
     * Caduca las entradas que llevan mas de una ventana completa sin usarse — a esa altura el
     * bucket ya se recargo del todo, asi que conservarlo no aporta nada.
     *
     * <p>Va aqui y no en el paquete scheduler porque es mantenimiento interno del limitador, no
     * una tarea de negocio.
     */
    @Scheduled(fixedDelay = 300_000)
    public void limpiarInactivos() {
        long limite = System.currentTimeMillis() - VENTANA.toMillis();
        int antes = buckets.size();
        buckets.entrySet().removeIf(e -> e.getValue().ultimoAccesoMillis < limite);
        int eliminadas = antes - buckets.size();
        if (eliminadas > 0) {
            log.debug("Rate limiter: {} entradas inactivas eliminadas, quedan {}", eliminadas, buckets.size());
        }
    }
}
