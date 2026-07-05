package com.ventas.key.mis.productos.models.promociones;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PromocionRequestDto {
    private String descripcion;
    private LocalDateTime fechaVencimiento;
    private List<PromocionDetalleRequestDto> detalles;
}
