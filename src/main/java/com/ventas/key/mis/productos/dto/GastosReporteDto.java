package com.ventas.key.mis.productos.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GastosReporteDto {

    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    // Ventas
    private Double totalVentas;
    private Double totalGananciaProductos;
    private Long totalTransacciones;

    // Gastos
    private Double totalGastos;
    private Map<String, Double> gastosPorCategoria;

    // Resultado
    private Double gananciaNeta;
}
