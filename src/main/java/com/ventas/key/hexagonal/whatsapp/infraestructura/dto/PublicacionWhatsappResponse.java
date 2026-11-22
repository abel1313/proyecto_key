package com.ventas.key.hexagonal.whatsapp.infraestructura.dto;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.PublicacionWhatsapp;

import java.time.LocalDateTime;

/** Una publicacion tal como la ve la pantalla (lista y detalle). */
public record PublicacionWhatsappResponse(int id, Integer varianteId, String grupo, String texto, String codigoRef, String modoVenta,
                                          String estado, LocalDateTime programadaPara, LocalDateTime fechaPublicacion, int intentos,
                                          String ultimoError, boolean tieneImagen) {

    public static PublicacionWhatsappResponse de(PublicacionWhatsapp p) {
        return new PublicacionWhatsappResponse(p.id(), p.varianteId(), p.grupo(), p.texto(), p.codigoRef(), p.modoVenta().name(),
                p.estado().name(), p.programadaPara(), p.fechaPublicacion(), p.intentos(), p.ultimoError(), p.imagenId() != null);
    }
}
