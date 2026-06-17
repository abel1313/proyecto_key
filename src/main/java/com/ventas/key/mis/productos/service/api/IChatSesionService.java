package com.ventas.key.mis.productos.service.api;

import com.ventas.key.mis.productos.entity.ChatSesion;

import java.util.List;
import java.util.Optional;

public interface IChatSesionService {

    String conectar(String ip, String nombreUsuario);

    void cerrarSesion(String sesionId);

    void actualizarActividad(String sesionId);

    List<ChatSesion> obtenerSesionesActivas();

    Optional<ChatSesion> buscarSesionActiva(String sesionId);

    void cerrarSesionesInactivas();
}
