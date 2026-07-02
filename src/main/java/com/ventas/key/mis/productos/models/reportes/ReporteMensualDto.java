package com.ventas.key.mis.productos.models.reportes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReporteMensualDto {
    private String mes; // "yyyy-MM"
    private Double totalVenta;
    private Double totalGanancia;
    private Long cantidadVentas;
    private List<ReporteDiarioDto> porDia;
}
