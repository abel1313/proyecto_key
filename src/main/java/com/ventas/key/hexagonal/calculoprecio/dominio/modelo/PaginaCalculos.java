package com.ventas.key.hexagonal.calculoprecio.dominio.modelo;

import java.util.List;

/** Una página del listado de cálculos (página desde 1). */
public record PaginaCalculos(List<CalculoDeProducto> items, long total, int pagina, int tamano) {
    public int totalPaginas() {
        return tamano <= 0 ? 0 : (int) Math.ceil((double) total / tamano);
    }
}
