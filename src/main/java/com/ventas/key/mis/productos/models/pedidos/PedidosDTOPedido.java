package com.ventas.key.mis.productos.models.pedidos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PedidosDTOPedido {
    private ClienteDTOPedido cliente;
    private LocalDate fechaPedido;
    private LocalDate fechaRecogida;
    private String estadoPedido;
    private String observaciones;
    private String tipoPedido;
    private String nombreReceptor;
    private String direccionEntrega;
    private List<DetallePedidosDTOPedido> detalles;
}
