package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RamoArmadoResponseDto {
    private Integer id;
    private String nombre;
    private String imagenUrl;
    private Integer tipoFlorId;
    private String tipoFlorNombre;
    private Integer colorFlorId;
    private String colorFlorNombre;
    private Integer colorFlorVarianteId;
    private Integer cantidad;
    private Double precioFlores;
    private Boolean papelIncluido;
    private Double precioPapel;
    private Integer papelVarianteId;
    private List<RamoArmadoAccesorioResponseDto> accesorios;
    private Double precioTotal;
    private Boolean activo;
}
