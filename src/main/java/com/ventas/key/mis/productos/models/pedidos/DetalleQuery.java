package com.ventas.key.mis.productos.models.pedidos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DetalleQuery {
    private Long producto;
    private String nombre_producto;
    private Integer cantidad;
    private Double precio_unitario;
    private Double sub_total;
}
