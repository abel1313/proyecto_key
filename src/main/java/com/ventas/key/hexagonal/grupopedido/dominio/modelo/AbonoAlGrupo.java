package com.ventas.key.hexagonal.grupopedido.dominio.modelo;

import com.ventas.key.hexagonal.grupopedido.dominio.excepcion.AbonoAlGrupoInvalidoException;

/**
 * Un pago que el titular hace por todo el grupo.
 *
 * @param montoCentavos     lo que se abona
 * @param metodoPago        {@code EFECTIVO} o {@code TRANSFERENCIA}
 * @param montoDadoCentavos con cuanto pago en efectivo, para el cambio; null si no aplica
 * @param nota              texto libre
 */
public record AbonoAlGrupo(long montoCentavos, String metodoPago, Long montoDadoCentavos, String nota) {

    /** Igual que el abono normal: el credito no se cobra con tarjeta (genera comision). */
    public AbonoAlGrupo {
        String metodo = metodoPago == null || metodoPago.isBlank() ? "EFECTIVO" : metodoPago.trim().toUpperCase();
        if (!"EFECTIVO".equals(metodo) && !"TRANSFERENCIA".equals(metodo)) {
            throw new AbonoAlGrupoInvalidoException("Los abonos solo se reciben en EFECTIVO o TRANSFERENCIA");
        }
    }

    public String metodo() {
        return metodoPago == null || metodoPago.isBlank() ? "EFECTIVO" : metodoPago.trim().toUpperCase();
    }

    /**
     * El cambio se valida contra el abono completo y no contra cada parte del reparto: el cliente
     * entrega un solo billete para todo el grupo.
     */
    public long cambioCentavos() {
        if (!"EFECTIVO".equals(metodo()) || montoDadoCentavos == null) {
            return 0;
        }
        if (montoDadoCentavos < montoCentavos) {
            throw new AbonoAlGrupoInvalidoException(String.format(
                    "El monto entregado $%.2f es menor al abono de $%.2f",
                    montoDadoCentavos / 100.0, montoCentavos / 100.0));
        }
        return montoDadoCentavos - montoCentavos;
    }
}
