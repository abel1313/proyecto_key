package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Lo que la publicacion necesita saber del articulo: como se llama, a cuanto se vende, si hay existencias y cual es su foto principal
 * ({@code null} si no tiene: entonces se publica solo el texto).
 */
public record ProductoParaPublicar(String nombre, Double precio, boolean disponible, Long imagenPrincipalId) { }
