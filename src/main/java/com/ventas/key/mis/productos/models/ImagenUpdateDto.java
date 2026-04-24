package com.ventas.key.mis.productos.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImagenUpdateDto extends ImagenDTO{
    private Long id;

    // usado por JPQL (Imagen.base64 es String en DB)
    public ImagenUpdateDto(Long id,
                           String base64Str,
                           String extension,
                           String nombre) {
        super(base64Str != null ? base64Str.getBytes() : null, extension, nombre);
        this.id = id;
    }

    public ImagenUpdateDto(Long id,
                           byte[] base64,
                           String extension,
                           String nombre) {
        super(base64, extension, nombre);
        this.id = id;
    }
}
