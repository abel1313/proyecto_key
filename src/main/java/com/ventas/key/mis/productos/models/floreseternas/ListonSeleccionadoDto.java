package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Cada entrada = un liston elegido (el cliente puede pedir varios, cada uno con su propia frase).
// Debe venir exactamente uno de los dos campos: fraseListonPredefinidaId O fraseListonPersonalizada.
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListonSeleccionadoDto {
    private Integer fraseListonPredefinidaId;
    private String fraseListonPersonalizada;
}
