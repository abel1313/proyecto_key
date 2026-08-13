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
    // Total a cobrar por papel (ya multiplicado por los pliegos si aplica).
    private Double precioPapel;
    private Integer papelVarianteId;
    // Cuantos pliegos se usaron para calcular precioPapel -- null si el papel no tiene
    // floresPorPliego configurado (precio fijo unico, retrocompatible) o no aplico papel.
    // Informativo: se recalcula en cada lectura con la config vigente del accesorio "es papel",
    // no queda congelado como precioPapel/precioTotal (ver RamoArmadoServiceImpl.papelVarianteId).
    private Integer pliegosPapel;
    // precioUnitario EXACTO para la linea de /v1/pedidos/savePedido de esta variante (cantidad =
    // pliegosPapel o 1 si es null) -- mismo motivo que CalcularPrecioResponseDto.precioUnitarioPapel.
    private Double precioUnitarioPapel;
    private List<RamoArmadoAccesorioResponseDto> accesorios;
    private Double precioTotal;
    private Boolean activo;
}
