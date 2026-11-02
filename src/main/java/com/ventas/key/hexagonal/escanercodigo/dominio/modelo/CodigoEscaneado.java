package com.ventas.key.hexagonal.escanercodigo.dominio.modelo;

/** Un código que el celular mandó. {@code secuencia} crece de 1 en 1 dentro de la conexión: la PC pide «los que sean mayores a N». */
public record CodigoEscaneado(long secuencia, String codigo) {
}
