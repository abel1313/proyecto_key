package com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida;

/**
 * Bloqueos, cooldown y límite de mensajes por hora (el mismo control del chat del sitio).
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]</p>
 */
public interface ControlDeAbusoWhatsappPort {

    boolean bloqueado(String clave);

    boolean limiteExcedido(String clave);

    void registrarMensaje(String clave);

    void registrarNoEntendido(String clave);

    void registrarEntendido(String clave);
}
