package com.ventas.key.hexagonal.busquedapedido.dominio.puerto.entrada;

import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.FiltroPedidos;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.PaginaDePedidos;

/**
 * [Hexagonal: Driving Port] [Clean: Use Case Input Boundary]
 *
 * <p>La lista de pedidos del administrador con todos sus filtros.
 */
public interface BuscarPedidosCasoUso {

    PaginaDePedidos buscar(FiltroPedidos filtro);
}
