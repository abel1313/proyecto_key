package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RamoArmadoRequestDto {
    private String nombre;
    private Integer tipoFlorId;
    private Integer cantidadFlorValidaId;
    private List<RamoArmadoAccesorioRequestDto> accesorios;
    private Boolean activo;
}
