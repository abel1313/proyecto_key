package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/** Un mensaje del hilo que se le pasa al chatbot para que no pierda el contexto. */
public record TurnoDeConversacion(Rol rol, String texto) {

    public enum Rol {
        CLIENTE,
        BOT,
        PERSONA
    }
}
