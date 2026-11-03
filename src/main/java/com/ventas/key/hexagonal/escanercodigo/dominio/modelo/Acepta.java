package com.ventas.key.hexagonal.escanercodigo.dominio.modelo;

/** Qué sabe recibir la pantalla de la PC que pregunta: códigos de barras, fotos o las dos. Así una foto no se «entrega» a una pantalla sin dónde ponerla. */
public record Acepta(boolean codigos, boolean fotos) {

    public static final Acepta SOLO_CODIGOS = new Acepta(true, false);
}
