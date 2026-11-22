package com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.PublicacionWhatsapp;

import java.util.List;

/** [Hexagonal: Driving Port] [Clean: Use Cases] Lo que el bot de WhatsApp le pide al back sobre las publicaciones. */
public interface PublicacionesParaElBotCasoUso {

    /** Devuelve las que ya les toca salir y las pasa a EN_PROCESO en la misma transaccion. */
    List<PublicacionWhatsapp> recogerPendientes();

    /** JPEG de la publicacion; lanza {@code PublicacionWhatsappException} (404) si no existe o no tiene imagen. */
    byte[] imagenDe(int publicacionId);

    void marcarPublicada(int publicacionId);

    void marcarError(int publicacionId, String detalle);
}
