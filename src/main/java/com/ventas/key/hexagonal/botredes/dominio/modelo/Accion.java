package com.ventas.key.hexagonal.botredes.dominio.modelo;

/** Qué hace el bot con una interacción: qué contesta, si avisa al admin y si se pausa. */
public record Accion(String textoPublico, boolean avisarAdmin, boolean pausar, String motivo) {
}
