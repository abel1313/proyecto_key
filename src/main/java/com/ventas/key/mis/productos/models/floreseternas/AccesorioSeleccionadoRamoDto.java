package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Accesorio elegido en un ramo ya guardado, tal como quedo en el Pedido -- a diferencia de
// AccesorioSeleccionadoDto (una entrada por unidad, para el request de calcular-precio/editar),
// aqui ya viene agregado con su cantidad, pensado para reconstruir el configurador al editar.
// No incluye el papel (esPapel=true en AccesorioRamo): ese se recalcula solo segun la cantidad
// final de flores, no es una eleccion del cliente.
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccesorioSeleccionadoRamoDto {
    private Integer accesorioId;
    private String accesorioNombre;
    private Integer cantidad;
}
