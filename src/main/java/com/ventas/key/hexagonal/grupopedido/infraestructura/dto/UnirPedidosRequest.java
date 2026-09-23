package com.ventas.key.hexagonal.grupopedido.infraestructura.dto;

import lombok.Data;

import java.util.List;

/** {@code POST /v1/grupos-pedido} */
@Data
public class UnirPedidosRequest {
    private List<Integer> pedidoIds;
    /** El pedido cuyo cliente paga y recoge. Tiene que ser uno de {@code pedidoIds}. */
    private Integer pedidoTitularId;
    private String nota;
}
