package com.ventas.key.hexagonal.preferenciafiltro.dominio.puerto.entrada;

import com.ventas.key.hexagonal.preferenciafiltro.dominio.modelo.FiltrosGuardados;

import java.util.Optional;

/**
 * [Hexagonal: Driving Port] [Clean: Use Case Input Boundary]
 *
 * <p>Siempre sobre el usuario de la petición (R3): ningún método recibe un usuario.
 */
public interface FiltrosGuardadosCasoUso {

    Optional<FiltrosGuardados> obtener(String clavePantalla);

    /** Vacío si se mandó {@code {}}: eso borra lo guardado (R7). */
    Optional<FiltrosGuardados> guardar(String clavePantalla, String filtrosJson);

    void borrar(String clavePantalla);
}
