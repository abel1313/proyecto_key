package com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada;

/** [Hexagonal: Driving Port] [Clean: Use Case Input Boundary] */
public interface ReconciliarCobrosCasoUso {

    /**
     * Vuelve a preguntarle a la pasarela por los cobros que se dieron por perdidos (rechazados,
     * cancelados o vencidos) en las ultimas horas, por si alguno si se cobro (aprobacion tardia o
     * respuesta perdida al crearlo). Lo llama un trabajo programado.
     *
     * @return cuantos terminaron aplicandose o marcandose como cobrados
     */
    int reconciliar();
}
