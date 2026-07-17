package com.ventas.key.mis.productos.models.reportes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PromocionReporteDto {
    private Integer promocionId;
    private String descripcion;
    private Long combosVendidos;
    private Long numeroTransacciones;
    private Double ventaTotal;
    private Double gananciaTotal;
    private LocalDate ultimaVenta;
}
