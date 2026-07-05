package com.ventas.key.mis.productos.models.promociones;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PromocionResponseDto {
    private Integer id;
    private String descripcion;
    private LocalDateTime fechaVencimiento;
    private Boolean activo;
}
