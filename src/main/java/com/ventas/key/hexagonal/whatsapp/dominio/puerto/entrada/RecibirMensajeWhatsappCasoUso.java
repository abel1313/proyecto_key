package com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.EstadoDeEntrega;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MensajeEntrante;

/**
 * Lo que Meta le avisa a la aplicación por el webhook.
 *
 * <p>[Hexagonal: Driving Port] [Clean: Use Case Input Boundary]</p>
 */
public interface RecibirMensajeWhatsappCasoUso {

    /** Un cliente escribió: se guarda (una sola vez por id de Meta), se abre la ventana y, si toca, contesta el bot. */
    void recibir(MensajeEntrante mensaje);

    /** Meta avisa que un mensaje nuestro llegó, se leyó o falló. */
    void registrarEstadoDeEntrega(EstadoDeEntrega estado);
}
