package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListonCalculadoDto {
    private String texto;
    // "PREDEFINIDA" (precio conocido de inmediato) o "PERSONALIZADA_PENDIENTE" (sin precio
    // hasta que el admin la valide y le asigne uno)
    private String tipo;
    private Double precio;
    // Variante "sombra" de la frase predefinida elegida -- null si es PERSONALIZADA_PENDIENTE
    // (todavia no puede venderse como linea real, no tiene precio ni variante asociada).
    private Integer varianteId;
}
