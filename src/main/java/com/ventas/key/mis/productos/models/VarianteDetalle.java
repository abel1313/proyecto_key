package com.ventas.key.mis.productos.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class VarianteDetalle {

    private Integer id;
    private Integer productoId;
    private String talla;
    private String descripcion;
    private String color;
    private String presentacion;
    private int stock;
    private String marca;
    private String contenidoNeto;
    private List<ImagenDTO> listImagenes = new ArrayList<>();
}