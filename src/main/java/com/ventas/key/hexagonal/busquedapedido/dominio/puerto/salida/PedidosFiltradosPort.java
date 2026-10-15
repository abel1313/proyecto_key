package com.ventas.key.hexagonal.busquedapedido.dominio.puerto.salida;

import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.FiltroPedidos;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.PaginaDePedidos;

import java.time.LocalDate;

/**
 * [Hexagonal: Driven Port] [Clean: Interface Adapter]
 *
 * <p>Aplica el filtro sobre los pedidos guardados. {@code hoy} llega de afuera para que "hoy",
 * "mañana" y "atrasados" no dependan del reloj del servidor (R7).
 */
public interface PedidosFiltradosPort {

    PaginaDePedidos buscar(FiltroPedidos filtro, LocalDate hoy);
}
