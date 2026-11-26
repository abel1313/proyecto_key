package com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MensajeWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.Pagina;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.PlantillaWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ResumenDeConversacion;

/**
 * Lo que hace una persona del negocio desde la pantalla: ver conversaciones, contestar a mano y
 * decidir quién atiende (ella o el bot).
 *
 * <p>[Hexagonal: Driving Port] [Clean: Use Case Input Boundary]</p>
 */
public interface AdministrarConversacionesWhatsappCasoUso {

    Pagina<ResumenDeConversacion> listar(int pagina, int size);

    ResumenDeConversacion conversacion(long clienteId);

    /** Historial de la conversación, lo más nuevo primero. */
    Pagina<MensajeWhatsapp> mensajes(long clienteId, int pagina, int size);

    /**
     * Contesta a mano con texto libre. Solo con la ventana de 24 h abierta; si no, queda el motivo en
     * el historial y lanza {@code EnvioRechazadoException}. Al contestar, el bot se aparta (regla R10).
     */
    MensajeWhatsapp responder(long clienteId, String texto);

    /** Manda una plantilla aprobada (la única forma de escribir fuera de la ventana). Requiere método de pago. */
    MensajeWhatsapp enviarPlantilla(long clienteId, PlantillaWhatsapp plantilla);

    /** Toma la conversación: el bot no contesta hasta que se la devuelvan. */
    ResumenDeConversacion tomar(long clienteId);

    /** Devuelve la conversación al bot. */
    ResumenDeConversacion devolverAlBot(long clienteId);

    /** Quita la marca "requiere atención" sin contestar. */
    ResumenDeConversacion marcarAtendida(long clienteId);
}
