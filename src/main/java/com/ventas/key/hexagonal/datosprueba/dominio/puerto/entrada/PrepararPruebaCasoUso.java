package com.ventas.key.hexagonal.datosprueba.dominio.puerto.entrada;

import com.ventas.key.hexagonal.datosprueba.dominio.modelo.EscenarioDePrueba;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.EscenarioPreparado;

import java.util.List;

/** [Hexagonal: Driving Port] Dar de alta lo que necesita una prueba de QA: pedidos (R16) y catálogo (R18). */
public interface PrepararPruebaCasoUso {

    List<EscenarioDePrueba> disponibles();

    /** Crea lo que necesita la prueba {@code prueba} ("21") y regresa qué quedó. Solo en QA (R1). */
    EscenarioPreparado preparar(String prueba, int usuarioId);

    /** R19: todas las pruebas de una vez; si una falla, las demás siguen y esa trae su error. */
    List<EscenarioPreparado> prepararTodas(int usuarioId);
}
