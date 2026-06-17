package com.ventas.key.mis.productos.models.chat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatEventoAdmin {
    private String tipo;         // NUEVA_SESION | MENSAJE
    private String sesionId;
    private String nombreUsuario;
    private String contenido;
    private String timestamp;
}
