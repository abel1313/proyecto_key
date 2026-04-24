package com.ventas.key.mis.productos.models.variantes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VarianteDto {

    private Integer id;
    private String talla;
    private String descripcion;
    private String color;
    private String presentacion;
    private int stock;
    private String marca;
    private String contenidoNeto;
    private double precio;
    private String codigoBarras;
}
