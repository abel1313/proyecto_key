package com.ventas.key.hexagonal.busquedapedido.dominio.modelo;

import java.util.List;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Los numeros de pedido de una pagina, en el orden pedido, y cuantos hay en total.
 */
public record PaginaDePedidos(List<Integer> pedidoIds, long totalRegistros, int pagina, int tamano) {

    public PaginaDePedidos {
        pedidoIds = pedidoIds == null ? List.of() : List.copyOf(pedidoIds);
    }

    public int totalPaginas() {
        return tamano <= 0 ? 0 : (int) ((totalRegistros + tamano - 1) / tamano);
    }
}
