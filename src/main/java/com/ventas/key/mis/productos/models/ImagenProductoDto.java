package com.ventas.key.mis.productos.models;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
    @JsonSerialize(using = ToStringSerializer.class)
    private Long idImagen;
    private String name;
    private double price;
    private String extencion;
    private String image;
    private String inventoryStatus;
}
