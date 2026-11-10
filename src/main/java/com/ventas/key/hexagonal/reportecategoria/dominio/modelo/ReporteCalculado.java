package com.ventas.key.hexagonal.reportecategoria.dominio.modelo;

import java.util.List;

public record ReporteCalculado(FiltroReporte filtro, List<FilaReporte> filas, TotalesReporte totales) {
}
