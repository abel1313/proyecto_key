package com.ventas.key.mis.productos.models;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ImagenDTO {

    private byte[] base64;
    private String extension;
    private String nombreImagen;
}
