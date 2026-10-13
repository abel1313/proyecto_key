package com.ventas.key.hexagonal.datosprueba.dominio.puerto.entrada;

import com.ventas.key.hexagonal.datosprueba.dominio.modelo.EscenarioDePrueba;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.EscenarioPreparado;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.PruebaQuitada;

import java.util.List;

/**
 * [Hexagonal: Driving Port] Dar de alta lo que necesita una prueba de QA: pedidos (R16) y catálogo (R18),
 * y dejarla en blanco para volver a empezar (R21).
 */
public interface PrepararPruebaCasoUso {

    List<EscenarioDePrueba> disponibles();

    /**
     * Deja lista la prueba {@code prueba} ("21") y regresa qué quedó. Si ya se había preparado, primero
     * quita lo de la vez anterior (R21): así tocar el botón otra vez es reiniciar la prueba. Solo en QA (R1).
     */
    EscenarioPreparado preparar(String prueba, int usuarioId);

    /** R19: todas las pruebas de una vez, cada una reiniciada; si una falla, las demás siguen y esa trae su error. */
    List<EscenarioPreparado> prepararTodas(int usuarioId);

    /**
     * R21: quita lo de esa prueba sin volver a crearlo. Los pedidos se cancelan con el servicio real (el
     * stock regresa) y los modelos y artículos se dan de baja; nada se borra. Solo en QA (R1).
     */
    PruebaQuitada quitar(String prueba);

    /** R21: quita lo de todas las pruebas; si una falla, las demás siguen y esa trae su error. */
    List<PruebaQuitada> quitarTodas();
}
