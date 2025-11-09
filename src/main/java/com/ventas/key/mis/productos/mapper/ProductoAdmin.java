package com.ventas.key.mis.productos.mapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ProductoAdmin extends ProductoUser {

    private Double precioCosto;
    private Double piezas;
    private Double precioRebaja;
    private Integer stock;
    private String marca;
    private String contenido;
    private char habilitado;
}
