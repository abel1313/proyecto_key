package com.ventas.key.hexagonal.whatsapp.infraestructura.entrada.rest.dto;

/** Lo que escribe la persona en la pantalla. Máximo 4096 caracteres (límite de WhatsApp). */
public record ResponderRequest(String texto) {
}
