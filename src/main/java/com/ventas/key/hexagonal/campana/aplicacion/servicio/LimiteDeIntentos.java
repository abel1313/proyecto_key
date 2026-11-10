package com.ventas.key.hexagonal.campana.aplicacion.servicio;

import com.ventas.key.hexagonal.campana.dominio.excepcion.CampanaInvalidaException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Frena el adivinar códigos: máximo {@code maximo} intentos por quien pregunta dentro de la ventana. Java puro. */
public class LimiteDeIntentos {

    private final Clock reloj;
    private final int maximo;
    private final Duration ventana;
    private final Map<String, Deque<Instant>> intentos = new ConcurrentHashMap<>();

    public LimiteDeIntentos(Clock reloj, int maximo, Duration ventana) {
        this.reloj = reloj;
        this.maximo = maximo;
        this.ventana = ventana;
    }

    public void registrar(String quien) {
        Deque<Instant> cola = intentos.computeIfAbsent(quien == null ? "?" : quien, k -> new ArrayDeque<>());
        synchronized (cola) {
            Instant ahora = reloj.instant();
            while (!cola.isEmpty() && cola.peekFirst().isBefore(ahora.minus(ventana))) {
                cola.pollFirst();
            }
            if (cola.size() >= maximo) {
                throw new CampanaInvalidaException("Demasiados intentos con códigos. Espera unos minutos e inténtalo de nuevo.");
            }
            cola.addLast(ahora);
        }
    }
}
