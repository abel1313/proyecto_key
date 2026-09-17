package com.ventas.key.mis.productos.service.api;

public interface IChatNotificacionService {

    void notificarNuevaSesion(String sesionId, String nombreUsuario);

    void notificarMensaje(String sesionId, String nombreUsuario, String contenido);

    // El bot dejo de atender esa conversacion y ahora te toca a ti. `motivo` es lo que se pone en
    // el asunto para que se vea en la bandeja sin abrir el correo.
    void notificarEscalado(String sesionId, String nombreUsuario, String motivo, String ultimoMensaje);

    // El bot no pudo contestar por una falla al llamar a OpenAI (credito agotado, llave vencida,
    // el servicio caido). Va aparte del escalado normal: aqui hay algo que revisar, no solo un
    // cliente esperando.
    void notificarFallaDelBot(String sesionId, String nombreUsuario, String detalle);

    void marcarAdminConectado(String wsSessionId);

    void marcarAdminDesconectado();

    boolean isAdminConectado();

    boolean isAdminSession(String wsSessionId);
}
