package com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida;

import com.ventas.key.hexagonal.grupopedido.dominio.modelo.RegistroGrupo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Guarda quien esta en cada grupo. No guarda montos: esos se leen siempre de los pedidos.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 */
public interface GrupoPedidosPort {

    /** Crea el grupo con sus pedidos y devuelve su id. */
    Integer crear(Integer pedidoTitularId, List<Integer> pedidoIds, String nota, Integer usuarioId);

    Optional<RegistroGrupo> buscar(Integer grupoId);

    Optional<Integer> grupoActivoDe(Integer pedidoId);

    /** pedidoId -> grupo activo, solo para los que estan en uno. */
    Map<Integer, Integer> gruposActivosDe(Collection<Integer> pedidoIds);

    void cambiarTitular(Integer grupoId, Integer pedidoTitularId);

    /** El grupo deja de estar activo. Los pedidos no se tocan. */
    void marcarDeshecho(Integer grupoId, Integer usuarioId);
}
