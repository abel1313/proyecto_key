package com.ventas.key.hexagonal.campana.dominio.modelo;

import java.math.BigDecimal;

/**
 * [Hexagonal: Domain] Respuesta al «Verificar» de Venta directa. {@code NO_EXISTE}: ese código no es de nadie. {@code NO_COINCIDE}: existe pero
 * es de otra persona (no coincide el correo ni el teléfono del cliente de la venta). {@code OK}: es de este cliente.
 */
public record VerificacionDeCodigo(Estado estado, String nombre, BigDecimal acumulado, BigDecimal meta) {

    public enum Estado { NO_EXISTE, NO_COINCIDE, OK }

    /** Lo que le falta para la meta con lo que ya lleva (0 si ya la alcanzó o no hay meta). */
    public BigDecimal falta() {
        if (estado != Estado.OK || meta == null || meta.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal f = meta.subtract(acumulado);
        return f.signum() < 0 ? BigDecimal.ZERO : f;
    }
}
