package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/** Si se puede enviar, y si no, por qué (el motivo se deja en el historial). */
public sealed interface DecisionDeEnvio {

    record Enviar() implements DecisionDeEnvio {
    }

    record NoEnviar(EstadoMensaje estado, String motivo) implements DecisionDeEnvio {
    }
}
