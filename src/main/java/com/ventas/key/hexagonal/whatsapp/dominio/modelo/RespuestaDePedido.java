package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/**
 * Lo que el bot le manda al cliente por el tema del pedido.
 *
 * @param requiereAtencion marca la conversación para una persona
 * @param borradorPendiente si no es null, el texto es el resumen de ese borrador: SOLO se guarda (y empieza a valer el
 *                          «sí») cuando el resumen de verdad salió hacia el cliente (regla R16)
 * @param texto vacío = no se manda nada (se ignora el mensaje)
 */
public record RespuestaDePedido(String texto, boolean requiereAtencion, BorradorDePedido borradorPendiente) {

    public RespuestaDePedido(String texto, boolean requiereAtencion) {
        this(texto, requiereAtencion, null);
    }

    public static RespuestaDePedido ignorar() {
        return new RespuestaDePedido("", false, null);
    }

    public boolean esIgnorar() {
        return texto == null || texto.isBlank();
    }
}
