package com.ventas.key.mis.productos.chatbot;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ChatbotRequest {

    @NotBlank
    @Size(max = 1000)
    private String mensaje;

    // Lo manda el navegador: se limita para que no se inventen conversaciones enormes (gasto de tokens) ni mensajes nulos.
    @Size(max = 20)
    @Valid
    private List<MensajeHistorial> historial;

    // Lo manda el navegador para hilar los mensajes de una misma conversacion. Viene vacio en el
    // primer mensaje: el back crea la sesion y devuelve el id para que el front lo reuse.
    private String sesionId;

    @Data
    public static class MensajeHistorial {
        @NotNull
        @Size(max = 20)
        private String rol;      // "user" o "assistant"
        @NotNull
        @Size(max = 1000)
        private String contenido;
    }
}