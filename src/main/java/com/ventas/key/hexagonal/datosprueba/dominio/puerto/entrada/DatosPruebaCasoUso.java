package com.ventas.key.hexagonal.datosprueba.dominio.puerto.entrada;

import com.ventas.key.hexagonal.datosprueba.dominio.modelo.Avance;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.PlanDeDatos;

/** [Hexagonal: Driving Port] [Clean: Use Case Input Boundary] */
public interface DatosPruebaCasoUso {

    /**
     * Revisa el ambiente (R1) y que no haya otra corrida (R3), y arranca en segundo plano.
     * Regresa en cuanto arranca, con el primer avance.
     *
     * @param usuarioId quien la arranca: queda como el usuario de las ventas de prueba
     */
    Avance iniciar(PlanDeDatos plan, int usuarioId);

    /** Como va la corrida actual o como termino la ultima. */
    Avance avance();

    /** R14: da de baja los modelos y articulos de prueba. Regresa cuantos articulos quedaron de baja. */
    int darDeBaja();
}
