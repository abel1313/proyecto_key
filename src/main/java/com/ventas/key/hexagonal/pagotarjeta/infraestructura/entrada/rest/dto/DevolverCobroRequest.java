package com.ventas.key.hexagonal.pagotarjeta.infraestructura.entrada.rest.dto;

import java.math.BigDecimal;

/** @param monto lo que se devuelve; vacio = todo lo que falte por devolver */
public record DevolverCobroRequest(BigDecimal monto) {
}
