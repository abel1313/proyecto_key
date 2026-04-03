package com.ventas.key.mis.productos.hexagonal.infraestructura.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ImagenDto {
    private Long id;
    private String nombreImagen;
    private byte[] imagen;
    private String contentType;
}
