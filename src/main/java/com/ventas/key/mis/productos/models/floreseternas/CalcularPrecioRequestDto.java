package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalcularPrecioRequestDto {
    // Uno o varios colores de LA MISMA especie (TipoFlor) -- ej. [{colorFlorId:1,cantidad:6},
    // {colorFlorId:2,cantidad:6}] para un ramo mitad rojo, mitad blanco. La cantidad final del
    // ramo es la suma de todas las entradas.
    private List<ColorSeleccionadoDto> colores;
    private List<AccesorioSeleccionadoDto> accesorios;
    private List<ListonSeleccionadoDto> listones;
    private Integer lugarEntregaId;
    private Boolean recogerEnLocal;
}
