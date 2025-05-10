package com.ventas.key.mis.productos.models;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TotalDetalle {

    private BigDecimal id;
    private String nombreUsuario;
    private String nombreProducto;
    private BigDecimal cant;
    private BigDecimal precioUnitario;
    private BigDecimal subTotal;
    private BigDecimal total;


}
