package com.ventas.key.hexagonal.campana.dominio.modelo;

import java.math.BigDecimal;
import java.util.Map;

/**
 * [Hexagonal: Domain] Lo que decidió la campaña para esta venta: a qué producto le toca qué precio de campaña (solo los que SÍ bajan), de qué
 * campaña viene y a qué persona se liga la compra ({@code candidata} = persona nueva que se guardará al cerrar la venta).
 */
public record DecisionDeCampana(Integer campanaId, String nombreCampana, Map<Integer, BigDecimal> preciosPorProducto, PersonaCampana persona,
                                PersonaCampana candidata, BigDecimal ticketSinJuguetes, boolean recurrente,
                                Integer clienteId, Integer clienteSinRegistroId, String correoDeLaVenta) {

    /** Sin el correo de la venta (el código se manda al de la persona). */
    public DecisionDeCampana(Integer campanaId, String nombreCampana, Map<Integer, BigDecimal> preciosPorProducto, PersonaCampana persona,
                             PersonaCampana candidata, BigDecimal ticketSinJuguetes, boolean recurrente,
                             Integer clienteId, Integer clienteSinRegistroId) {
        this(campanaId, nombreCampana, preciosPorProducto, persona, candidata, ticketSinJuguetes, recurrente, clienteId, clienteSinRegistroId, null);
    }

    /** Sin el cliente de la venta. */
    public DecisionDeCampana(Integer campanaId, String nombreCampana, Map<Integer, BigDecimal> preciosPorProducto, PersonaCampana persona,
                             PersonaCampana candidata, BigDecimal ticketSinJuguetes, boolean recurrente) {
        this(campanaId, nombreCampana, preciosPorProducto, persona, candidata, ticketSinJuguetes, recurrente, null, null);
    }

    public static final DecisionDeCampana NINGUNA =
            new DecisionDeCampana(null, null, Map.of(), null, null, BigDecimal.ZERO, false);

    public DecisionDeCampana {
        preciosPorProducto = preciosPorProducto == null ? Map.of() : Map.copyOf(preciosPorProducto);
    }

    public boolean hayCampana() {
        return campanaId != null;
    }

    public BigDecimal precioDe(int productoId) {
        return preciosPorProducto.get(productoId);
    }
}
