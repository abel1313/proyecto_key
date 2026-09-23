package com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida;

import com.ventas.key.hexagonal.grupopedido.dominio.modelo.PedidoDelGrupo;

import java.util.Collection;
import java.util.List;

/**
 * Lee los pedidos tal como estan hoy.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 */
public interface PedidosDelGrupoPort {

    /** Los que existan de esos ids; los que no existen simplemente no vienen. */
    List<PedidoDelGrupo> buscar(Collection<Integer> pedidoIds);
}
