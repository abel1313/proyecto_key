package com.ventas.key.hexagonal.campana.infraestructura.entrada.rest.dto;

/** {@code POST /v1/campanas/personas/buscar-por-codigo}: el UUID que salió del QR (escrito, escaneado o leído de una imagen). */
public record BuscarPorCodigoRequest(String codigo) {
}
