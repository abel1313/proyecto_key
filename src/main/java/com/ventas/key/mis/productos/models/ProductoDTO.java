package com.ventas.key.mis.productos.models;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ProductoDTO {
    private String nombre;
    private Double precioCosto;
    private Double piezas;
    private String color;
    private Double precioVenta;
    private Double precioRebaja;
    private String descripcion;
    private Integer stock;
    private String marca;
    private String contenido;
    private String codigoBarras;
    private int idProducto;
}
