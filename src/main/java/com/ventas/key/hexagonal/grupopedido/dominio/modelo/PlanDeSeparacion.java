package com.ventas.key.hexagonal.grupopedido.dominio.modelo;

import java.util.List;
import java.util.Map;

/**
 * Como queda todo despues de separar.
 *
 * @param salen     los pedidos que dejan el grupo (si queda uno solo, tambien sale)
 * @param quedan    los que siguen unidos; vacio si el grupo se termina
 * @param titular   quien recoge a los que siguen unidos; null si el grupo se termina
 * @param objetivos pedidoId -> centavos abonados con los que queda cada pedido; vacio en un grupo de contado
 */
public record PlanDeSeparacion(
        List<Integer> salen,
        List<Integer> quedan,
        Integer titular,
        Map<Integer, Long> objetivos) {

    public boolean terminaElGrupo() {
        return quedan.isEmpty();
    }
}
