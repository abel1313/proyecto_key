package com.ventas.key.hexagonal.grupopedido.infraestructura.dto;

import lombok.Data;

/** {@code PUT /v1/grupos-pedido/{grupoId}/titular} */
@Data
public class CambiarTitularRequest {
    private Integer pedidoTitularId;
}
