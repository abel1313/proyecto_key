package com.ventas.key.mis.productos.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalcularCostoEnvioResponse {
    // false = el punto quedo fuera de todos los anillos configurados para la zona -- el front
    // debe bloquear el avance del checkout en ese caso. Si la zona no tiene anillos configurados
    // (todavia sin migrar a este esquema), siempre da true con el costoEnvio fijo de la zona.
    private boolean dentroDeRango;
    private Double costoEnvio;
    private Integer anilloId;
}
