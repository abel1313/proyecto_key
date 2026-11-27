package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/** Por qué una conversación necesita a una persona (va en el aviso por correo). */
public enum MotivoDeEscalada {
    PIDIO_UNA_PERSONA("El cliente pidió hablar con una persona, o preguntó algo que el asistente no puede contestar"),
    MANDO_UN_ADJUNTO("El cliente mandó una foto, audio u otro archivo que el asistente no puede leer"),
    FALLO_EL_ASISTENTE("El asistente no pudo contestar (falló o dijo algo que no debe decir)");

    private final String descripcion;

    MotivoDeEscalada(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }
}
