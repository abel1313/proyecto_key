package com.ventas.key.mis.productos.service.api;

import com.ventas.key.mis.productos.entity.ChatSesion;

import java.util.List;
import java.util.Optional;

public interface IChatSesionService {

    // Quien atiende una conversacion del chat en vivo. Viven aqui, no en la implementacion, para
    // que los llame por nombre cualquiera que decida sobre el modo (ver ChatVivoBotService).
    String MODO_BOT = "BOT";
    String MODO_HUMANO = "HUMANO";

    String conectar(String ip, String nombreUsuario, Integer usuarioId);

    String asegurarSesionBot(String sesionId, String ip);

    void cerrarSesion(String sesionId);

    void actualizarActividad(String sesionId);

    List<ChatSesion> obtenerSesionesActivas();

    List<ChatSesion> obtenerSesionesRecientes();

    Optional<ChatSesion> buscarSesionActiva(String sesionId);

    // Sin filtrar por estado, para leer la conversacion tal como esta (incluidas las CERRADAS).
    Optional<ChatSesion> buscarSesion(String sesionId);

    Optional<ChatSesion> reactivarSesion(String sesionId);

    String modoDe(String sesionId);

    void cambiarModo(String sesionId, String modo);

    boolean existeSesion(String sesionId);

    void cerrarSesionesInactivas();
}
