package com.ventas.key.hexagonal.whatsapp.dominio.excepcion;

/** No existe esa conversación (cliente de WhatsApp). */
public class ConversacionNoEncontradaException extends RuntimeException {

    public ConversacionNoEncontradaException(long clienteId) {
        super("No existe la conversación " + clienteId);
    }
}
