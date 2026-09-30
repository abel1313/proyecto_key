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

    /**
     * Pedidos que se pueden unir con uno de forma de cobro {@code tipo}: no cancelados, no
     * cobrados de contado, y que no esten en un grupo activo. Del mas nuevo al mas viejo.
     *
     * @param excluir pedido que no debe salir (el que se esta viendo)
     * @param buscar  numero de pedido (empieza con) o nombre del cliente (contiene); vacio = todos
     */
    PaginaDePedidos candidatos(String tipo, Integer excluir, String buscar, int pagina, int tamano);

    /**
     * @param pedidos los de esta pagina
     * @param hayMas  si hay otra pagina despues de esta
     */
    record PaginaDePedidos(List<PedidoDelGrupo> pedidos, boolean hayMas) {
    }
}
