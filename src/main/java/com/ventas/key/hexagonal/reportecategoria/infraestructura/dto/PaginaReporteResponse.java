package com.ventas.key.hexagonal.reportecategoria.infraestructura.dto;

import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.TotalesReporte;

import java.util.List;

/** Una página del reporte en pantalla. Los totales son de TODAS las filas del reporte, no de la página. */
public record PaginaReporteResponse(String descripcion, List<FilaReporteResponse> filas, int pagina, int totalPaginas, int totalRegistros,
                                    TotalesReporte totales) {
}
