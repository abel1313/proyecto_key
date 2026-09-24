package com.ventas.key.hexagonal.botredes.dominio.modelo;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * El bot se aparta de una conversación cuando la atiende una persona: el admin contestó a mano o
 * el bot la escaló. La pausa dura {@code duracion} desde la última de esas dos cosas; pasado ese
 * tiempo el bot vuelve a contestar.
 */
public final class Pausa {

    private Pausa() {
    }

    public static boolean vigente(LocalDateTime desde, LocalDateTime ahora, Duration duracion) {
        return desde != null && ahora.isBefore(desde.plus(duracion));
    }
}
