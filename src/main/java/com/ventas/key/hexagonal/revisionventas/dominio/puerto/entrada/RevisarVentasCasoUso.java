package com.ventas.key.hexagonal.revisionventas.dominio.puerto.entrada;

import com.ventas.key.hexagonal.revisionventas.dominio.modelo.ResultadoRevision;

import java.time.LocalDate;

/** [Hexagonal: Driving Port] [Clean: Use Case Input Boundary] */
public interface RevisarVentasCasoUso {

    /**
     * Busca cosas raras en pedidos, ventas, abonos, stock y cobros con tarjeta. Solo lee.
     *
     * @param desde primer día a revisar en lo que depende de fecha (null = 90 días antes de {@code hasta})
     * @param hasta último día (null = hoy)
     */
    ResultadoRevision revisar(LocalDate desde, LocalDate hasta);
}
