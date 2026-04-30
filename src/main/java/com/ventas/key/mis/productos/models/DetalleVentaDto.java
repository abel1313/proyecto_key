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
    private Integer varianteId;
    private Integer cantidad;
    private Double precioVenta;
    private Double subTotal;
}