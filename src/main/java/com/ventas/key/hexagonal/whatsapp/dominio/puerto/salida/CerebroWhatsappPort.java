package com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.TurnoDeConversacion;

import java.util.List;

/**
 * El chatbot de la tienda atendiendo por WhatsApp. Si falla o tarda lanza la excepción y el
 * servicio pasa la conversación a una persona.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]</p>
 */
public interface CerebroWhatsappPort {

    /** Respuesta cruda del chatbot, con sus marcas internas ({@code ##HUMANO##}, etc.). */
    String responder(String mensaje, List<TurnoDeConversacion> historial, boolean esPrimeraVez);
}
