package com.ventas.key.hexagonal.pagotarjeta.dominio.modelo;

/**
 * Lo que paso al intentar aplicar un pago aprobado al pedido.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 */
public enum ResultadoAplicacion {
    /** El pedido quedo pagado con este cobro. */
    APLICADO,
    /**
     * El pedido ya no admite este pago: ya estaba pagado (por otro cobro o en efectivo), esta cancelado,
     * cambio de forma de cobro, ya tenia abonos o su total cambio. El dinero cobrado queda por devolver.
     */
    NO_APLICABLE
}
