package com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida;

import java.util.Optional;

/** [Hexagonal: Driven Port] [Clean: Interface Adapter] La imagen que el bot sube al grupo, ya lista (JPEG liviano). */
public interface ImagenDePublicacionPort {

    /** Vacio si la publicacion no tiene imagen o ya no existe en el servicio de imagenes. */
    Optional<byte[]> jpegLiviano(long imagenId);
}
