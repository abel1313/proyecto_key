package com.ventas.key.mis.productos.models.chat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SesionActivaDto {
    private String sesionId;
    private String nombreUsuario;
    private String estado;
    private String fechaInicio;
    private String ultimaActividad;
    private String ultimoMensaje;

    // Cuantos mensajes del cliente siguen sin respuesta. El panel del admin lo usa para el globito:
    // sin esto, un mensaje que llegaba con el panel cerrado no dejaba ninguna senal (el topic de
    // WebSocket no guarda nada) y la sesion se veia igual que una ya atendida.
    private long sinResponder;
}
