package com.ventas.key.hexagonal.pagotarjeta.dominio.modelo;

import java.math.BigDecimal;

/** Un plan de meses sin intereses (3, 6, 9 o 12) con su comision. */
public record PlanMsi(int meses, boolean activo, BigDecimal comision) {
}
