package com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ModoVenta;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.PublicacionWhatsapp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [Hexagonal: Driving Port] [Clean: Use Case]
 *
 * <p>Lo que hace el administrador desde la pantalla de publicaciones de WhatsApp: crear (o programar), ver, cancelar y reintentar.
 */
public interface PublicacionesAdminCasoUso {

    /** {@code texto} es opcional: sin el, se arma con la plantilla (nombre y precio del articulo). {@code programadaPara} vacio = ahora. */
    record NuevaPublicacion(Integer varianteId, String grupo, String texto, ModoVenta modoVenta, LocalDateTime programadaPara) { }

    PublicacionWhatsapp crear(NuevaPublicacion nueva);

    List<PublicacionWhatsapp> listar();

    PublicacionWhatsapp cancelar(int id);

    PublicacionWhatsapp reintentar(int id);
}
