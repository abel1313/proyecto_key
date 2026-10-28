package com.ventas.key.hexagonal.pagotarjeta.dominio.modelo;

import java.util.List;

/**
 * Lo que se le dice al front sobre los meses sin intereses de un pedido: los plazos que se pueden
 * ofrecer y, si no hay ninguno, <b>por qué</b> y qué artículos lo impiden. Así la pantalla puede decir
 * "este artículo no acepta meses sin intereses" y el cobro sigue de contado.
 *
 * @param meses            plazos disponibles (3, 6, 9, 12); vacío = solo de contado
 * @param motivo           por qué no hay meses (null cuando sí hay)
 * @param articulosSinMeses nombres de los artículos que no aceptan meses (vacío si el motivo es otro)
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 */
public record OfertaDeMeses(List<Integer> meses, String motivo, List<String> articulosSinMeses) {

    public OfertaDeMeses {
        meses = meses == null ? List.of() : List.copyOf(meses);
        articulosSinMeses = articulosSinMeses == null ? List.of() : List.copyOf(articulosSinMeses);
    }

    public static OfertaDeMeses con(List<Integer> meses) {
        return new OfertaDeMeses(meses, null, List.of());
    }

    public static OfertaDeMeses sinMeses(String motivo) {
        return new OfertaDeMeses(List.of(), motivo, List.of());
    }

    public static OfertaDeMeses sinMeses(String motivo, List<String> articulos) {
        return new OfertaDeMeses(List.of(), motivo, articulos);
    }

    public boolean hayMeses() {
        return !meses.isEmpty();
    }
}
