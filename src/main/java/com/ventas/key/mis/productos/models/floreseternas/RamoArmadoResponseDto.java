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
    // Id de la variante "sombra" del ramo (no del color) -- usar este varianteId para subir/leer
    // las fotos del ramo YA ARMADO completo via el sistema de imagenes de variantes que ya existe:
    // POST /tienda/v1/guardarConImagenes y GET /tienda/v1/imagenes/{varianteId}. Esta variante
    // nunca se vende ni aparece en un pedido, solo sirve para colgarle fotos.
    private Integer varianteId;
    // Id del producto "sombra" detras de esa variante -- guardarConImagenes lo exige siempre
    // (VarianteDetalle.productoId), aunque sea una actualizacion sobre una variante existente.
    private Integer varianteProductoId;
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
    // Monto de mano de obra ya incluido en precioTotal -- informativo/interno, el front NO debe
    // mostrarlo como linea aparte al cliente (decision del dueno: "un solo precio de ramo", igual
    // que el papel automatico). Null si esta cantidad no tiene mano de obra configurada.
    private Double precioManoDeObra;
    private List<RamoArmadoAccesorioResponseDto> accesorios;
    private Double precioTotal;
    private Boolean activo;
}
