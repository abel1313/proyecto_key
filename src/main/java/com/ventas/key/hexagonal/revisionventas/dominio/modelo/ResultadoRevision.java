package com.ventas.key.hexagonal.revisionventas.dominio.modelo;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * El resultado completo de una revisión.
 *
 * @param revisados   un renglón por cada tipo que SÍ se pudo revisar (con 0 si todo está bien)
 * @param noRevisados tipos que no se pudieron revisar en este ambiente (por ejemplo, el cobro con tarjeta sin su tabla)
 */
public record ResultadoRevision(LocalDate desde, LocalDate hasta, Instant generadoEn,
                                List<HallazgosDeUnTipo> revisados, List<TipoHallazgo> noRevisados) {

    public ResultadoRevision {
        revisados = List.copyOf(revisados);
        noRevisados = List.copyOf(noRevisados);
    }

    public int totalHallazgos() {
        return revisados.stream().mapToInt(HallazgosDeUnTipo::total).sum();
    }

    public boolean todoEnOrden() {
        return totalHallazgos() == 0;
    }
}
