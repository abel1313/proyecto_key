package com.ventas.key.hexagonal.grupopedido.infraestructura.dto;

import lombok.Data;

/** {@code POST /v1/grupos-pedido/{grupoId}/abonos} */
@Data
public class AbonoGrupoRequest {
    private Double monto;
    /** {@code EFECTIVO} (default) o {@code TRANSFERENCIA}. */
    private String metodoPago;
    /** Con cuanto pago en efectivo, para calcular el cambio. */
    private Double montoDado;
    private String nota;
}
