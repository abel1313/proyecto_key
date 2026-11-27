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

    /**
     * Respuesta cruda del chatbot, con sus marcas internas ({@code ##HUMANO##}, {@code ##PEDIDO{..}##}, etc.).
     *
     * @param contextoDePedido datos de esta conversación para tomar un pedido (número del cliente, lo que ya dio), o
     *                         {@code null} si el bot no toma pedidos (regla R16): entonces el modelo ni sabe que existen
     */
    String responder(String mensaje, List<TurnoDeConversacion> historial, boolean esPrimeraVez, String contextoDePedido);
}
