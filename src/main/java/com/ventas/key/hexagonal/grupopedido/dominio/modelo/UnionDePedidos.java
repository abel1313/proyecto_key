package com.ventas.key.hexagonal.grupopedido.dominio.modelo;

import com.ventas.key.hexagonal.grupopedido.dominio.excepcion.PedidoNoAgrupableException;
import com.ventas.key.hexagonal.grupopedido.dominio.excepcion.PedidosDeDistintoTipoException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Las reglas para poder unir pedidos. Sin estado: recibe lo que hay y dice si se puede.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 */
public final class UnionDePedidos {

    private UnionDePedidos() {
    }

    /**
     * @param pedidoIds       los que se pidieron unir, en el orden en que llegaron
     * @param encontrados     los que existen de esos
     * @param pedidoTitularId el pedido cuyo cliente paga y recoge
     * @param grupoActivoDe   pedidoId -> grupo activo en el que ya esta, solo para los que ya estan
     */
    public static void validar(List<Integer> pedidoIds,
                               List<PedidoDelGrupo> encontrados,
                               Integer pedidoTitularId,
                               Map<Integer, Integer> grupoActivoDe) {
        Set<Integer> distintos = new LinkedHashSet<>(pedidoIds == null ? List.of() : pedidoIds);
        distintos.remove(null);

        // R1
        if (distintos.size() < 2) {
            throw new PedidoNoAgrupableException("Para unir se necesitan al menos 2 pedidos distintos");
        }

        Set<Integer> existentes = encontrados.stream().map(PedidoDelGrupo::pedidoId).collect(Collectors.toSet());
        List<Integer> faltantes = distintos.stream().filter(id -> !existentes.contains(id)).toList();
        if (!faltantes.isEmpty()) {
            throw new PedidoNoAgrupableException("No existen los pedidos " + faltantes);
        }

        // R5
        if (pedidoTitularId == null || !distintos.contains(pedidoTitularId)) {
            throw new PedidoNoAgrupableException("El titular tiene que ser uno de los pedidos que se van a unir");
        }

        // R2
        for (PedidoDelGrupo pedido : encontrados) {
            if (!pedido.estaAbierto()) {
                throw new PedidoNoAgrupableException("El pedido #" + pedido.pedidoId() + " " + pedido.motivoDelCierre()
                        + ": solo se unen pedidos abiertos");
            }
        }

        // R4
        for (Integer id : distintos) {
            Integer grupo = grupoActivoDe.get(id);
            if (grupo != null) {
                throw new PedidoNoAgrupableException("El pedido #" + id + " ya esta en el grupo #" + grupo
                        + ": deshaz ese grupo primero");
            }
        }

        // R3
        long tipos = encontrados.stream().map(PedidoDelGrupo::tipo).distinct().count();
        if (tipos > 1) {
            Map<Integer, String> tipoPorPedido = new LinkedHashMap<>();
            for (Integer id : distintos) {
                encontrados.stream().filter(p -> p.pedidoId().equals(id)).findFirst()
                        .ifPresent(p -> tipoPorPedido.put(id, p.tipo()));
            }
            throw new PedidosDeDistintoTipoException(tipoPorPedido);
        }
    }
}
