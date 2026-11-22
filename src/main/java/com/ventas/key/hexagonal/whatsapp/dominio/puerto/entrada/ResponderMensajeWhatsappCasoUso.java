package com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MensajeWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.RespuestaBot;

/** [Hexagonal: Driving Port] [Clean: Use Case] Decide que hace el bot con un mensaje de WhatsApp. */
public interface ResponderMensajeWhatsappCasoUso {

    RespuestaBot atender(MensajeWhatsapp mensaje);
}
