package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalcularPrecioRequestDto {
    private Integer tipoFlorId;
    private Integer cantidadFinal;
    private List<AccesorioSeleccionadoDto> accesorios;
    private List<ListonSeleccionadoDto> listones;
    private Integer lugarEntregaId;
    private Boolean recogerEnLocal;
}
