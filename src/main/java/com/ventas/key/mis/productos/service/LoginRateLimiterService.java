package com.ventas.key.mis.productos.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimiterService {

    // Un Bucket por IP — máx 5 intentos cada 15 minutos
    private static final int MAX_INTENTOS = 5;
    private static final Duration VENTANA = Duration.ofMinutes(15);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket crearBucket() {
        Bandwidth limite = Bandwidth.builder()
                .capacity(MAX_INTENTOS)
                .refillIntervally(MAX_INTENTOS, VENTANA)
                .build();
        return Bucket.builder().addLimit(limite).build();
    }

    /**
     * Intenta consumir 1 token para la IP dada.
     * @return true si aún hay intentos disponibles, false si se excedió el límite.
     */
    public boolean tryConsume(String ipAddress) {
        Bucket bucket = buckets.computeIfAbsent(ipAddress, k -> crearBucket());
        return bucket.tryConsume(1);
    }

    /**
     * Devuelve los segundos restantes hasta que se recarguen los tokens.
     */
    public long segundosHastaRecarga(String ipAddress) {
        Bucket bucket = buckets.computeIfAbsent(ipAddress, k -> crearBucket());
        return bucket.getAvailableTokens() == 0
                ? VENTANA.getSeconds()
                : 0;
    }
}
