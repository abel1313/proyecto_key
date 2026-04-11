package com.ventas.key.mis.productos.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ImagenProductoBase64 {
    private Integer idProducto;
    private Long idImagen;
    private String name;
    private double price;
    private String extencion;
    private byte[] image;
    private String inventoryStatus;
}
