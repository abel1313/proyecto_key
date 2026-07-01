package com.ventas.key.mis.productos.models;

import lombok.Data;

@Data
public class NotificacionRequest {
    private boolean enviarCorreo;
    private boolean enviarWhatsapp;
    /** HTML del ticket generado por el front — se envía por correo */
    private String ticketHtml;
    /** Texto plano del ticket generado por el front — se envía por WhatsApp */
    private String ticketTexto;
}
