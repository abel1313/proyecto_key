package com.ventas.key.hexagonal.grupopedido.aplicacion.servicio;

import com.ventas.key.hexagonal.grupopedido.dominio.modelo.GrupoPedidos;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.entrada.ConsultarGruposCasoUso;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.GrupoPedidosPort;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.PedidosDelGrupoPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * En que grupo activo esta cada pedido, para marcarlo en la lista.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Use Case interactor]
 *
 * <p>Una pagina de la lista trae 10 pedidos: se resuelve con una consulta de membresia y una
 * lectura por grupo distinto, no con una por pedido.
 */
@Service
@RequiredArgsConstructor
public class ConsultarGruposService implements ConsultarGruposCasoUso {

    private final GrupoPedidosPort grupos;
    private final PedidosDelGrupoPort pedidos;

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, GrupoPedidos> gruposActivosDe(Collection<Integer> pedidoIds) {
        Map<Integer, Integer> grupoDe = grupos.gruposActivosDe(pedidoIds);
        Map<Integer, GrupoPedidos> porGrupo = new HashMap<>();
        for (Integer grupoId : new HashSet<>(grupoDe.values())) {
            grupos.buscar(grupoId).ifPresent(registro ->
                    porGrupo.put(grupoId, GrupoPedidos.de(registro, pedidos.buscar(registro.pedidoIds()))));
        }
        Map<Integer, GrupoPedidos> resultado = new HashMap<>();
        grupoDe.forEach((pedidoId, grupoId) -> {
            GrupoPedidos grupo = porGrupo.get(grupoId);
            if (grupo != null) {
                resultado.put(pedidoId, grupo);
            }
        });
        return resultado;
    }
}
