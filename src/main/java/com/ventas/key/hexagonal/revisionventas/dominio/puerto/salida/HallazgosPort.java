package com.ventas.key.hexagonal.revisionventas.dominio.puerto.salida;

import com.ventas.key.hexagonal.revisionventas.dominio.modelo.BusquedaDeHallazgos;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.TipoHallazgo;

import java.time.LocalDate;

/**
 * De dónde salen los datos de la revisión. SOLO LECTURA.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 */
public interface HallazgosPort {

    /**
     * @param limiteEjemplos cuántos casos concretos devolver como máximo (el total siempre es el real)
     * @throws com.ventas.key.hexagonal.revisionventas.dominio.excepcion.RevisionVentasException.RevisionNoDisponible
     *         si este tipo no se puede revisar en este ambiente
     */
    BusquedaDeHallazgos buscar(TipoHallazgo tipo, LocalDate desde, LocalDate hasta, int limiteEjemplos);
}
