package com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MensajeWhatsapp;

/** [Hexagonal: Driven Port] [Clean: Interface Adapter] Avisa al admin que una conversacion de WhatsApp necesita a una persona (hoy, por correo). */
public interface AvisoAdminWhatsappPort {

    /** {@code publicacionCodigo} puede ser null (chat privado sin publicacion). */
    void necesitaAtencion(MensajeWhatsapp mensaje, String publicacionCodigo, String motivo);
}
