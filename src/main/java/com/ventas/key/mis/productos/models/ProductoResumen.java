package com.ventas.key.mis.productos.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ProductoResumen {
    private int idProducto;
    private String nombre;
    private String descripcion;
    private int stock;
    private Double getPrecioVenta;
    private String codigoBarras;
}
