package com.ventas.key.mis.productos.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ImagenProductoDto {
    private Integer idProducto;
    private Integer idImagen;
    private String name;
    private double price;
    private String extencion;
    private byte[] image;
    private String inventoryStatus;
}
