package com.ventas.key.hexagonal.grupopedido.infraestructura.dto;

import lombok.Data;

/** {@code POST /v1/grupos-pedido/{grupoId}/cobrar-contado} */
@Data
public class CobroContadoRequest {
    /** La misma opcion de pago que manda "Cobrar" en un pedido suelto. */
    private Integer pagosYMesesId;
}
