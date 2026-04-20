package com.ventas.key.mis.productos.hexagonal.dominio;

import lombok.*;

import java.io.Serializable;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Imagen implements Serializable {

    private static final long serialVersionUID = 1L;


    private Long id;
    private String nombreImagen;
    private byte[] imagen;
    private String urlImagen;
    private String contentType;
}
