package com.ventas.key.mis.productos.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DetalleVentaDto {

    private String nombre;
    private String descripcion;
    private Integer stock;
    private Double precioVenta;
    private String codigoBarras;
    private Integer cantidad;
    private Double subTotal;
}
