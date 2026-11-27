package com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MotivoDeEscalada;

/**
 * Avisa al administrador, fuera de la pantalla, que una conversación lo necesita (regla R13). Nunca debe
 * lanzar excepción: si el aviso falla, la conversación sigue igual (el correo es un extra, no parte de la respuesta).
 * [Hexagonal: Driven Port]
 */
public interface AvisoAlAdministradorPort {

    void conversacionNecesitaPersona(String telefono, String nombre, String ultimoMensaje, MotivoDeEscalada motivo);
}
