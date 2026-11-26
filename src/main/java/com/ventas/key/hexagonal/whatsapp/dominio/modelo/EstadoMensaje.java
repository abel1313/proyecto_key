package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import java.util.Optional;

/**
 * En qué quedó un mensaje. Los tres últimos "no enviado" dejan en el historial el MOTIVO por el que
 * no salió nada (regla R4): fuera de la ventana de 24 h, sin método de pago para la plantilla, o
 * respuesta del bot que quedó pendiente de plantilla o de una persona.
 */
public enum EstadoMensaje {
    RECIBIDO(0),
    ENVIADO(1),
    ENTREGADO(2),
    LEIDO(3),
    FALLIDO(9),
    BLOQUEADO_VENTANA(9),
    BLOQUEADO_SIN_PAGO(9),
    PENDIENTE_PLANTILLA(9);

    private final int rango;

    EstadoMensaje(int rango) {
        this.rango = rango;
    }

    /** El mensaje sí llegó a salir hacia el cliente (se cuenta como "ya le habíamos escrito"). */
    public boolean salioAlCliente() {
        return this == ENVIADO || this == ENTREGADO || this == LEIDO;
    }

    /**
     * Meta avisa los estados en desorden (a veces "read" llega antes que "delivered") y repite
     * avisos: un estado nunca retrocede, y uno que terminó mal no vuelve a cambiar.
     */
    public boolean puedeCambiarA(EstadoMensaje nuevo) {
        if (rango >= 9 || nuevo == null) {
            return false;
        }
        if (nuevo == FALLIDO) {
            return this == ENVIADO;
        }
        return nuevo.rango < 9 && nuevo.rango > rango;
    }

    /** "sent", "delivered", "read" y "failed" son los estados que manda Meta. */
    public static Optional<EstadoMensaje> deMeta(String estado) {
        if (estado == null) {
            return Optional.empty();
        }
        return switch (estado) {
            case "sent" -> Optional.of(ENVIADO);
            case "delivered" -> Optional.of(ENTREGADO);
            case "read" -> Optional.of(LEIDO);
            case "failed" -> Optional.of(FALLIDO);
            default -> Optional.empty();
        };
    }
}
