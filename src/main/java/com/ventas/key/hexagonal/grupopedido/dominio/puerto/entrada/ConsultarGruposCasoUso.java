package com.ventas.key.hexagonal.grupopedido.dominio.puerto.entrada;

import com.ventas.key.hexagonal.grupopedido.dominio.modelo.GrupoPedidos;

import java.util.Collection;
import java.util.Map;

/**
 * Solo lectura: en que grupo activo esta cada pedido.
 *
 * <p>[Hexagonal: Driving Port] [Clean: Use Case input boundary]
 *
 * <p>Va separado de {@link UnirPedidosCasoUso} porque lo usa la lista de pedidos
 * ({@code PedidoServiceImpl}), y {@code UnirPedidosService} a su vez depende de ese servicio para
 * cobrar: juntarlos cerraria un ciclo de dependencias.
 */
public interface ConsultarGruposCasoUso {

    /** pedidoId -> su grupo activo, solo para los que estan en uno. */
    Map<Integer, GrupoPedidos> gruposActivosDe(Collection<Integer> pedidoIds);
}
