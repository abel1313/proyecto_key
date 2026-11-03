package com.ventas.key.hexagonal.escanercodigo.dominio.modelo;

/** Una foto que el celular mandó: JPEG en base64 con su encabezado {@code data:image/jpeg;base64,}. {@code secuencia} comparte el contador con los códigos. */
public record FotoRecibida(long secuencia, String foto) {
}
