package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Un color elegido y cuantas flores de ese color -- el cliente puede mandar una sola entrada
// (ramo de un solo color) o varias (ramo mezclado, ej. 6 rojas + 6 blancas).
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ColorSeleccionadoDto {
    private Integer colorFlorId;
    private Integer cantidad;
}
