package com.ventas.key.mis.productos.mapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ProductoUser {

    private String nombre;
    private String color;
    private Double precioVenta;
    private String descripcion;
    private String codigoBarras;
    private int idProducto;

}
