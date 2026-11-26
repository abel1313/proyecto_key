package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import java.time.Instant;

/** Una conversación como se ve en la lista de la pantalla. {@code ventanaCierraEn} es null si la ventana ya está cerrada. */
public record ResumenDeConversacion(ClienteWhatsapp cliente, boolean botPausado, boolean ventanaAbierta,
                                    Instant ventanaCierraEn, long sinResponder) {
}
