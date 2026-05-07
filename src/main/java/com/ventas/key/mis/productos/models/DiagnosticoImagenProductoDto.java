package com.ventas.key.mis.productos.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DiagnosticoImagenProductoDto {

    private Integer productoId;
    private String nombreProducto;
    private int totalImagenesLocalDB;
    private List<ImagenDiagnosticoItem> imagenesLocalDB;
    private boolean imagenPresenteEnMicroservicio;
    private String detalleExternoLista;
}
