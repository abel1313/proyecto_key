package com.ventas.key.mis.productos.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DiagnosticoImagenVarianteDto {

    private Integer varianteId;
    private int totalImagenesLocalDB;
    private List<ImagenDiagnosticoItem> imagenesLocalDB;
    private List<Long> idsConDatosEnMicroservicio;
    private List<Long> idsSinDatosEnMicroservicio;
    private boolean consistente;
}
