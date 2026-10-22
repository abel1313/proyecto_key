package com.ventas.key.hexagonal.pagotarjeta.dominio.modelo;

/** Un cobro tal como lo conoce la pasarela: su identificador externo y su estado. */
public record CobroEnPasarela(String idExterno, EstadoExterno estado) {
}
