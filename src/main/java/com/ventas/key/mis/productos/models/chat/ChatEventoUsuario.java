package com.ventas.key.mis.productos.models.chat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatEventoUsuario {
    private String tipo;       // MENSAJE | SESION_CERRADA
    private String remitente;  // ADMIN
    private String contenido;
    private String timestamp;
}
