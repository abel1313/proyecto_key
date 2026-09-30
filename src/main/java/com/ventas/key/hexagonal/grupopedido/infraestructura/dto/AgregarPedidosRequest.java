package com.ventas.key.hexagonal.grupopedido.infraestructura.dto;

import lombok.Data;

import java.util.List;

/** {@code POST /v1/grupos-pedido/{grupoId}/pedidos} */
@Data
public class AgregarPedidosRequest {
    private List<Integer> pedidoIds;
}
