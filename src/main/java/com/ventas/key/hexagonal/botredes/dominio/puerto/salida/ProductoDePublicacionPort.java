package com.ventas.key.hexagonal.botredes.dominio.puerto.salida;

import java.util.Optional;

/**
 * El producto de una publicación, si se publicó desde el panel admin ({@code publicacion_social}).
 * Si se subió directo en Facebook o Instagram, no hay.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]</p>
 */
public interface ProductoDePublicacionPort {

    Optional<Integer> varianteDe(String publicacionId);
}
