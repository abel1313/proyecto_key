package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/** Lo que el bot va a mandar, ya limpio de marcas internas. {@code Escalar} además pasa la conversación a una persona. */
public sealed interface RespuestaDelBot {

    record Contestar(String texto) implements RespuestaDelBot {
    }

    record Escalar(String texto, MotivoDeEscalada motivo) implements RespuestaDelBot {
    }
}
