package com.ventas.key.mis.productos.models.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardResumenDto {
    private Double ventasHoy;
    private Double ventasMes;
    private Double gananciaMes;
    private Double gastosMes;
    private Double gananciaNetaMes;
    private Long pedidosPendientesEntregar;
    private Long creditosActivos;
    private Double montoPorCobrar;
    private Long productosStockBajo;
}
