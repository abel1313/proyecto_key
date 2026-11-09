package com.ventas.key.hexagonal.calculoprecio.infraestructura.dto;

import com.ventas.key.hexagonal.calculoprecio.dominio.modelo.ComisionTerminal;

import java.math.BigDecimal;

/** Una comisión de terminal para elegir en la calculadora (tarifa + IVA, en % del precio). */
public record ComisionTerminalResponse(int pagosYMesesId, String descripcion, BigDecimal tarifaPct, BigDecimal ivaPct, BigDecimal totalPct) {
    public static ComisionTerminalResponse de(ComisionTerminal c) {
        return new ComisionTerminalResponse(c.pagosYMesesId(), c.descripcion(), c.tarifaPct(), c.ivaPct(), c.totalPct());
    }
}
