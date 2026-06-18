package com.ventas.key.mis.productos.models.chat;

import lombok.Data;

@Data
public class ChatAdminResponderRequest {
    private String sesionId;
    private String contenido;
}
