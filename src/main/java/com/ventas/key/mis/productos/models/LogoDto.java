package com.ventas.key.mis.productos.models;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LogoDto {
    private Integer id;
    private String extension;
    private String nombreOriginal;
    private boolean activo;
    private LocalDateTime creadoEn;
    /** Ruta relativa -- GET /logos/{id}/imagen (pública, sin login). */
    private String urlImagen;
}
