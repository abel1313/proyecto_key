package com.ventas.key.hexagonal.preferenciafiltro.dominio.puerto.salida;

import com.ventas.key.hexagonal.preferenciafiltro.dominio.modelo.FiltrosGuardados;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.modelo.Pantalla;

import java.util.Optional;

/**
 * [Hexagonal: Driven Port] [Clean: Interface Adapter]
 */
public interface FiltrosGuardadosPort {

    Optional<FiltrosGuardados> buscar(int usuarioId, Pantalla pantalla);

    /** Crea o reemplaza (R6). */
    FiltrosGuardados guardar(FiltrosGuardados filtros);

    void borrar(int usuarioId, Pantalla pantalla);
}
