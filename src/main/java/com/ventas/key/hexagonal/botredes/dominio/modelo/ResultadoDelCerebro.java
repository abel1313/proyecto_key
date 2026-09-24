package com.ventas.key.hexagonal.botredes.dominio.modelo;

/** Lo que devolvió el chatbot, ya interpretado. */
public sealed interface ResultadoDelCerebro {

    record Contestar(String texto) implements ResultadoDelCerebro {
    }

    /** El bot no tiene el dato o la pregunta no le toca: lo atiende una persona. */
    record Escalar() implements ResultadoDelCerebro {
    }

    /**
     * Saludo, halago o aviso. {@code frase} es el agradecimiento que propuso el chatbot según lo que
     * comentaron ("¡Muchas gracias por compartir! 💖"); el saludo lo pone PoliticaDeRespuesta.
     */
    record Agradecer(String frase) implements ResultadoDelCerebro {
    }

    record NoEntendido() implements ResultadoDelCerebro {
    }
}
