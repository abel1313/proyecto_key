package com.ventas.key.hexagonal.datosprueba.dominio.modelo;

import java.util.List;

/**
 * Lo que quedó listo para una prueba y lo que hay que hacer después.
 *
 * @param pedidos  los pedidos que se crearon (R16)
 * @param catalogo los modelos y artículos que se dieron de alta (R18)
 * @param error    null si salió bien; en "Preparar todas" (R19), el motivo si esa prueba falló
 */
public record EscenarioPreparado(String prueba, String titulo, List<PedidoPreparado> pedidos,
                                 List<CatalogoPreparado> catalogo, String siguientePaso, String error) {

    public EscenarioPreparado(String prueba, String titulo, List<PedidoPreparado> pedidos, String siguientePaso) {
        this(prueba, titulo, pedidos, List.of(), siguientePaso, null);
    }

    public static EscenarioPreparado conError(EscenarioDePrueba escenario, String motivo) {
        return new EscenarioPreparado(escenario.prueba(), escenario.titulo(), List.of(), List.of(), null, motivo);
    }
}
