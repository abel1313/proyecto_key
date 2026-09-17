package com.ventas.key.mis.productos.chatbot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ChatbotRequest {

    @NotBlank
    @Size(max = 1000)
    private String mensaje;

    private List<MensajeHistorial> historial;

    @Data
    public static class MensajeHistorial {
        private String rol;      // "user" o "assistant"
        private String contenido;
    }
}