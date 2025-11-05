package com.ventas.key.mis.productos.models.pedidos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PedidoGenerico {

    private ClienteQuery cliente;
    private PedidoQuery pedido;
}
