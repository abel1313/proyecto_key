package com.ventas.key.mis.productos.service.api;

import com.ventas.key.mis.productos.entity.ChatMensaje;

import java.util.List;
import java.util.Optional;

public interface IChatMensajeService {

    ChatMensaje guardar(String sesionId, String remitente, String contenido);

    List<ChatMensaje> obtenerHistorial(String sesionId);

    Optional<ChatMensaje> ultimoMensaje(String sesionId);
}
