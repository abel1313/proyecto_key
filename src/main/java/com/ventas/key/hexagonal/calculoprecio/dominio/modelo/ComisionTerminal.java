package com.ventas.key.hexagonal.calculoprecio.dominio.modelo;

import java.math.BigDecimal;

/**
 * [Clean: Entity] Lo que se queda la terminal de tarjeta por una forma de pago (contado, 3 meses sin intereses…): la tarifa más el IVA de la
 * terminal, los dos en % del precio. Es el catálogo que ya usa el cobro; la calculadora solo lo lee.
 */
public record ComisionTerminal(int pagosYMesesId, String descripcion, BigDecimal tarifaPct, BigDecimal ivaPct) {

    public ComisionTerminal {
        tarifaPct = tarifaPct == null ? BigDecimal.ZERO : tarifaPct;
        ivaPct = ivaPct == null ? BigDecimal.ZERO : ivaPct;
    }

    /** Comisión total que se descuenta del precio cobrado (tarifa + IVA), igual que al registrar la venta. */
    public BigDecimal totalPct() {
        return tarifaPct.add(ivaPct);
    }
}
