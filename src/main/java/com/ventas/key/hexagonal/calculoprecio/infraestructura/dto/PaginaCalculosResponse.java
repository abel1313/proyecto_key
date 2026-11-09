package com.ventas.key.hexagonal.calculoprecio.infraestructura.dto;

import com.ventas.key.hexagonal.calculoprecio.dominio.modelo.PaginaCalculos;

import java.util.List;

/** Una página del listado de cálculos (página desde 1). */
public record PaginaCalculosResponse(int pagina, int totalPaginas, long totalRegistros, List<CalculoDeProductoResponse> items) {
    public static PaginaCalculosResponse de(PaginaCalculos p) {
        return new PaginaCalculosResponse(p.pagina(), p.totalPaginas(), p.total(), p.items().stream().map(CalculoDeProductoResponse::de).toList());
    }
}
