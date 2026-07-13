package com.ventas.key.mis.productos.models.resenas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResenaEditarDto {
    private Integer calificacion;
    private String comentario;
}
