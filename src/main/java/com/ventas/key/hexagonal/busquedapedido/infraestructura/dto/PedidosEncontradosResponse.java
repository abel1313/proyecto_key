package com.ventas.key.hexagonal.busquedapedido.infraestructura.dto;

import com.ventas.key.mis.productos.models.pedidos.PedidoGenerico;

import java.util.List;

/**
 * [Hexagonal: Driving Adapter] [Clean: Interface Adapter]
 *
 * <p>{@code list} y {@code totalPaginas} se llaman igual que en {@code buscarClientePedido}
 * ({@code PageableDto}) para que el front reuse su modelo; {@code totalRegistros} y {@code pagina}
 * son nuevos (para el contador "N pedidos").
 */
public record PedidosEncontradosResponse(
        List<PedidoGenerico> list,
        int totalPaginas,
        long totalRegistros,
        int pagina) {
}
