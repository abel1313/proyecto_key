package com.ventas.key.mis.productos.dto.negocio;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ImagenPresentacionUpdateDto {
    private byte[] base64;
    private String extension;
    private String nombreImagen;
    private String descripcion;
    private Boolean activo;
}