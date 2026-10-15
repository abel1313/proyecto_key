package com.ventas.key.hexagonal.entrega.dominio.modelo;

import com.ventas.key.hexagonal.entrega.dominio.excepcion.EntregaNoPermitidaException;

import java.util.List;

/**
 * La entrega de un pedido, o de todos los de su grupo si esta unido (E7: entregar a uno es
 * entregar a todos, igual que el pago).
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Reglas (PLAN_PEDIDOS_VENTAS_ENTREGA.md §11):
 * <ul>
 *   <li>E5: la entrega va aparte del pago; aqui no se cobra nada.</li>
 *   <li>E8: un Apartado o un contado no se entregan sin estar pagados; un Ir pagando si (es justo
 *       llevarselo e ir pagando).</li>
 *   <li>Un pedido cancelado no se entrega. Los cancelados de un grupo se ignoran.</li>
 *   <li>E6: regresar a "Falta entregar" solo si ya estaba entregado.</li>
 * </ul>
 */
public final class Entrega {

    private final Integer pedidoId;
    private final List<PedidoParaEntregar> pedidos;

    public Entrega(Integer pedidoId, List<PedidoParaEntregar> pedidos) {
        if (pedidos == null || pedidos.isEmpty()) {
            throw new EntregaNoPermitidaException("No existe el pedido " + pedidoId);
        }
        this.pedidoId = pedidoId;
        this.pedidos = List.copyOf(pedidos);
    }

    private boolean esGrupo() {
        return pedidos.size() > 1;
    }

    private List<PedidoParaEntregar> vivos() {
        return pedidos.stream().filter(p -> !p.estaCancelado()).toList();
    }

    /** Los pedidos que hay que marcar como entregados. */
    public List<Integer> entregar() {
        List<PedidoParaEntregar> vivos = vivos();
        if (vivos.isEmpty()) {
            throw new EntregaNoPermitidaException(esGrupo()
                    ? "Todos los pedidos del grupo están cancelados"
                    : "El pedido " + pedidoId + " está cancelado: no se puede entregar");
        }
        if (vivos.stream().allMatch(PedidoParaEntregar::entregado)) {
            throw new EntregaNoPermitidaException(esGrupo()
                    ? "Los pedidos del grupo ya están entregados"
                    : "El pedido " + pedidoId + " ya está entregado");
        }
        for (PedidoParaEntregar p : vivos) {
            if (!p.esIrPagando() && !p.estaPagado()) {
                String quien = esGrupo() ? "El pedido " + p.pedidoId() + " del grupo" : "El pedido " + p.pedidoId();
                throw new EntregaNoPermitidaException(String.format(
                        "%s todavía no está pagado (falta $%.2f): primero se cobra y después se entrega",
                        quien, p.faltaCentavos() / 100.0));
            }
        }
        return vivos.stream().filter(p -> !p.entregado()).map(PedidoParaEntregar::pedidoId).toList();
    }

    /** Los pedidos que regresan a "Falta entregar". */
    public List<Integer> regresar() {
        List<Integer> entregados = vivos().stream()
                .filter(PedidoParaEntregar::entregado).map(PedidoParaEntregar::pedidoId).toList();
        if (entregados.isEmpty()) {
            throw new EntregaNoPermitidaException(esGrupo()
                    ? "Ningún pedido del grupo está entregado"
                    : "El pedido " + pedidoId + " no está entregado");
        }
        return entregados;
    }
}
