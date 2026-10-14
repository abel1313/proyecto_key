package com.ventas.key.hexagonal.codigopostal.dominio.puerto.salida;

import com.ventas.key.hexagonal.codigopostal.dominio.modelo.CodigoPostalInfo;

import java.util.Optional;

/**
 * [Hexagonal: Driven Port] [Clean: Interface Adapter]
 *
 * <p>Nuestra tabla de códigos postales ya consultados.
 */
public interface CodigoPostalGuardadoPort {

    Optional<CodigoPostalInfo> buscar(String codigo);

    /** R2 y R6: guarda o actualiza; si otra petición lo guardó al mismo tiempo, no falla. */
    void guardar(CodigoPostalInfo info);
}
