package com.ventas.key.mis.productos.models.chat;

import com.ventas.key.mis.productos.entity.ChatMensaje;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatHistorialPaginadoDto {
    private List<ChatMensaje> mensajes;
    private int pagina;
    private int totalPaginas;
    private long totalMensajes;
    private boolean hayMasAntiguos;
}
