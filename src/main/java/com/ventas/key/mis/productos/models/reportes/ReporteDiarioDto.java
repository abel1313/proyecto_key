package com.ventas.key.mis.productos.models.reportes;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReporteDiarioDto {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;
    private Double totalVenta;
    private Double totalGanancia;
    private Long cantidadVentas;
}
