package com.ventas.key.mis.productos.models.resenas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResenaRequestDto {
    private Integer varianteId;
    private Integer calificacion;
    private String comentario;
}
