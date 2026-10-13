package com.ventas.key.hexagonal.grupopedido.infraestructura.dto;

import lombok.Data;

/** {@code PUT /v1/grupos-pedido/{grupoId}/tipo} */
@Data
public class CambiarFormaCobroGrupoRequest {
    /** {@code APARTADO} o {@code FIADO} (Ir pagando). */
    private String tipoPedido;
    /** Lo que el cliente deja en este momento (solo Ir pagando); vacio o 0 si nada. */
    private Double monto;
    /** {@code EFECTIVO} (default) o {@code TRANSFERENCIA}. */
    private String metodoPago;
    /** Con cuanto pago en efectivo, para calcular el cambio. */
    private Double montoDado;
    private String nota;
}
