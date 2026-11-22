package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/** La decision del back para el bot. {@code publicacionId} y {@code modoVenta} solo vienen si el mensaje era de una publicacion. */
public record RespuestaBot(AccionBot accion, String respuesta, Integer publicacionId, ModoVenta modoVenta) {

    public static RespuestaBot ignorar() {
        return new RespuestaBot(AccionBot.IGNORAR, null, null, null);
    }
}
