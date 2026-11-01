package com.ventas.key.hexagonal.revisionventas.dominio.modelo;

import java.util.List;

/**
 * Lo que encontró la revisión de un tipo: cuántos hay en total y algunos ejemplos (no todos, para no saturar).
 */
public record HallazgosDeUnTipo(TipoHallazgo tipo, int total, List<Hallazgo> ejemplos) {

    public HallazgosDeUnTipo {
        ejemplos = ejemplos == null ? List.of() : List.copyOf(ejemplos);
    }

    public boolean hayAlgo() {
        return total > 0;
    }
}
