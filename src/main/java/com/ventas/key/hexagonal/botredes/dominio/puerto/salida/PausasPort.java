package com.ventas.key.hexagonal.botredes.dominio.puerto.salida;

import com.ventas.key.hexagonal.botredes.dominio.modelo.Canal;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Cuándo se apartó el bot de cada conversación ({@code comentario_pausa}: persona + publicación;
 * {@code mensaje_directo_pausa}: persona).
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]</p>
 */
public interface PausasPort {

    Optional<LocalDateTime> desde(Canal canal, String autorId, String publicacionId);

    /** Crea la pausa o, si ya existe, la reinicia a {@code cuando}. */
    void pausar(Canal canal, String autorId, String publicacionId, LocalDateTime cuando);
}
