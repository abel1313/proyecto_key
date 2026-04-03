package com.ventas.key.mis.productos.hexagonal.dominio;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Imagen {
    private Long id;
    private String nombreImagen;
    private byte[] imagen;
    private String contentType;
}
