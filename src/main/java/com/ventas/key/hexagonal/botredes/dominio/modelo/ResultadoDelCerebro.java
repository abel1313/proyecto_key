package com.ventas.key.hexagonal.botredes.dominio.modelo;

/** Lo que devolvió el chatbot, ya interpretado. */
public sealed interface ResultadoDelCerebro {

    record Contestar(String texto) implements ResultadoDelCerebro {
    }

    /** El bot no tiene el dato o la pregunta no le toca: lo atiende una persona. */
    record Escalar() implements ResultadoDelCerebro {
    }

    record NoEntendido() implements ResultadoDelCerebro {
    }
}
