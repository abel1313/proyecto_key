package com.ventas.key.hexagonal.escanercodigo.dominio.modelo;

import java.util.concurrent.CompletableFuture;

/**
 * Una pregunta «avísame cuando llegue un código» que la PC dejó abierta. {@code futuro} se completa en cuanto el celular manda algo
 * (o con {@code SesionNoDisponible} si la conexión se cierra); {@code cancelar} la quita si la PC se desconecta o se acaba el tiempo.
 */
public record EsperaDeCodigos(CompletableFuture<LecturaDeCodigos> futuro, Runnable cancelar) {
}
