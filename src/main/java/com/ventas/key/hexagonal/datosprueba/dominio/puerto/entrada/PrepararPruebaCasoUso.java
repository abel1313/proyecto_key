package com.ventas.key.hexagonal.datosprueba.dominio.puerto.entrada;

import com.ventas.key.hexagonal.datosprueba.dominio.modelo.EscenarioDePrueba;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.EscenarioPreparado;

import java.util.List;

/** [Hexagonal: Driving Port] Preparar los pedidos que necesita una prueba de QA (R16). */
public interface PrepararPruebaCasoUso {

    List<EscenarioDePrueba> disponibles();

    /** Crea los pedidos de la prueba {@code prueba} ("21") y regresa sus numeros. Solo en QA (R1). */
    EscenarioPreparado preparar(String prueba, int usuarioId);
}
