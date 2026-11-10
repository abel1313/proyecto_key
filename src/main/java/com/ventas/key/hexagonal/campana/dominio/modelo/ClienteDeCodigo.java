package com.ventas.key.hexagonal.campana.dominio.modelo;

import java.math.BigDecimal;

/**
 * [Hexagonal: Domain] Lo que se sabe de un código escaneado: si existe, a quién pertenece y cuánto lleva en la campaña vigente (si hay).
 * Con {@code valido = false}, {@code persona} es {@code null}.
 */
public record ClienteDeCodigo(boolean valido, PersonaCampana persona, BigDecimal acumulado, BigDecimal meta) {

    public static ClienteDeCodigo invalido() {
        return new ClienteDeCodigo(false, null, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /** Lo que le falta para la meta con lo que ya lleva (0 si ya la alcanzó o no hay meta). */
    public BigDecimal falta() {
        if (!valido || meta == null || meta.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal f = meta.subtract(acumulado);
        return f.signum() < 0 ? BigDecimal.ZERO : f;
    }
}
