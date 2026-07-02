package com.ventas.key.mis.productos.models.reportes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReporteClienteDto {
    private Integer clienteId;
    private String clienteNombre;
    private Long totalCompras;
    private Double totalGastado;
    private List<VentaResumenItem> ventas;
}
