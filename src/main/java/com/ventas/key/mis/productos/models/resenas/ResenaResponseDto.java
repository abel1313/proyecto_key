package com.ventas.key.mis.productos.models.resenas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResenaResponseDto {
    private Integer id;
    private Integer varianteId;
    private Integer calificacion;
    private String comentario;
    private LocalDateTime fechaCreacion;
    private String nombreCliente;
    private Boolean esPropia;
}
