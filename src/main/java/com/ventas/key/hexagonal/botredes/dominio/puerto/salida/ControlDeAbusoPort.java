package com.ventas.key.hexagonal.botredes.dominio.puerto.salida;

/**
 * Bloqueos, cooldown y límite de mensajes por hora ({@code ChatbotBlockService}).
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]</p>
 */
public interface ControlDeAbusoPort {

    boolean bloqueado(String clave);

    boolean limiteExcedido(String clave);

    void registrarMensaje(String clave);

    void registrarNoEntendido(String clave);

    void registrarEntendido(String clave);
}
