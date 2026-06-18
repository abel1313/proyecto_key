package com.ventas.key.mis.productos.service.api;

public interface IChatNotificacionService {

    void notificarNuevaSesion(String sesionId, String nombreUsuario);

    void notificarMensaje(String sesionId, String nombreUsuario, String contenido);

    void marcarAdminConectado(String wsSessionId);

    void marcarAdminDesconectado();

    boolean isAdminConectado();

    boolean isAdminSession(String wsSessionId);
}
