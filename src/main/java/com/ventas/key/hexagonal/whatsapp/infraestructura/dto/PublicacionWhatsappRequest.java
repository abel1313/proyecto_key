package com.ventas.key.hexagonal.whatsapp.infraestructura.dto;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ModoVenta;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada.PublicacionesAdminCasoUso.NuevaPublicacion;

import java.time.LocalDateTime;

/**
 * Lo que manda la pantalla al crear una publicacion. {@code texto} vacio = plantilla automatica; {@code modoVenta} vacio = PRIVADO;
 * {@code programadaPara} vacio = publicar ahora (formato ISO, p. ej. {@code 2026-11-25T18:00:00}).
 */
public record PublicacionWhatsappRequest(Integer varianteId, String grupo, String texto, String modoVenta, LocalDateTime programadaPara) {

    public NuevaPublicacion aDominio() {
        return new NuevaPublicacion(varianteId, grupo, texto, ModoVenta.desde(modoVenta), programadaPara);
    }
}
