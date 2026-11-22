package com.ventas.key.hexagonal.whatsapp.infraestructura.dto;

/**
 * Lo que recibe el bot por cada publicacion que le toca. {@code imagenPath} es relativo a la base del back
 * (por ejemplo {@code /v1/bot/publicaciones/12/imagen}); {@code null} si no hay imagen y entonces se publica solo el texto.
 */
public record PublicacionParaElBotResponse(int id, String grupo, String texto, String imagenPath, String codigoRef, String modoVenta) { }
