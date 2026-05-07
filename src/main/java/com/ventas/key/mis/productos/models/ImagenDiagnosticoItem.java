package com.ventas.key.mis.productos.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ImagenDiagnosticoItem {

    private Long imagenId;
    private String nombreImagen;
    private String extension;
    private String rutaDisco;
}
