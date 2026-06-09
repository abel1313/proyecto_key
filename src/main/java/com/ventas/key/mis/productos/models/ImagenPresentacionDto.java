package com.ventas.key.mis.productos.models;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ImagenPresentacionDto {
    private Integer id;
    private String tipo;
    private Integer orden;
    private String extension;
    private String nombreOriginal;
    private String descripcion;
    private boolean activo;
    private LocalDateTime actualizadoEn;
    /** URL apunta a GET /presentacion/v1/imagenes/{id}/imagen — bytes vienen del micro de imágenes */
    private String urlImagen;
}
