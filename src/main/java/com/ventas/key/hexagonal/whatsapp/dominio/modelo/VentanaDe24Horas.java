package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import java.time.Duration;
import java.time.Instant;

/**
 * La ventana de atención de WhatsApp (regla R1 de {@code whatsapp/README.md}).
 *
 * <p>Se abre con cada mensaje ENTRANTE del cliente y se cuenta desde la fecha y hora de su ÚLTIMO
 * mensaje (la que manda Meta en el webhook), no desde nuestra respuesta. Con menos de 24 horas se
 * puede mandar mensaje libre; con 24 horas o más, solo una plantilla aprobada. Sin ningún mensaje
 * entrante la ventana está cerrada.</p>
 */
public final class VentanaDe24Horas {

    public static final Duration DURACION = Duration.ofHours(24);

    private VentanaDe24Horas() {
    }

    /** {@code true} si pasaron MENOS de 24 horas desde el último mensaje entrante. */
    public static boolean abierta(Instant ultimoEntrante, Instant ahora) {
        if (ultimoEntrante == null || ahora == null) {
            return false;
        }
        return Duration.between(ultimoEntrante, ahora).compareTo(DURACION) < 0;
    }

    /** Cuándo se cierra la ventana que abrió ese mensaje (puede estar ya en el pasado). */
    public static Instant cierraEn(Instant ultimoEntrante) {
        return ultimoEntrante == null ? null : ultimoEntrante.plus(DURACION);
    }
}
