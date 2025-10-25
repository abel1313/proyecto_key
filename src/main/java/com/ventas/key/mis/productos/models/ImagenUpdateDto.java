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
    private int id;

    public ImagenUpdateDto(int id,
                           byte base64[],
                           String extension,
                           String nombre) {
        super(base64, extension, nombre);
        this.id = id;
    }
}
