package com.ventas.key.hexagonal.botredes.dominio.puerto.salida;

import com.ventas.key.hexagonal.botredes.dominio.modelo.Interaccion;

/**
 * Avisa al admin que una conversación necesita a una persona (hoy, por correo).
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]</p>
 */
public interface AvisoAdminPort {

    void necesitaAtencion(Interaccion interaccion, String motivo);
}
