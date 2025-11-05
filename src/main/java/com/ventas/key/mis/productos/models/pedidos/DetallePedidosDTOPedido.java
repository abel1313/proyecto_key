package com.ventas.key.mis.productos.models.pedidos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DetallePedidosDTOPedido {

    private int id;
    private ProductoDTOPedidos producto;
    private Integer cantidad;
    private Double precioUnitario;
    private Double subTotal;
}
