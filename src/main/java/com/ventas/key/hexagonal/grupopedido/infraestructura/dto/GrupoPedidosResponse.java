package com.ventas.key.hexagonal.grupopedido.infraestructura.dto;

import com.ventas.key.hexagonal.grupopedido.dominio.modelo.GrupoPedidos;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.PedidoDelGrupo;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** El grupo con sus totales, calculados al momento desde los pedidos. */
public record GrupoPedidosResponse(
        Integer grupoId,
        boolean activo,
        Integer pedidoTitularId,
        String titularNombre,
        String tipoPedido,
        LocalDateTime fechaCreacion,
        String nota,
        double totalGrupo,
        double pagadoGrupo,
        double saldoGrupo,
        List<PedidoItem> pedidos) {

    public record PedidoItem(
            Integer pedidoId,
            String cliente,
            String tipoPedido,
            String estadoPedido,
            double total,
            double pagado,
            double saldo,
            boolean esTitular) {
    }

    public static GrupoPedidosResponse de(GrupoPedidos g) {
        List<PedidoItem> items = g.pedidos().stream()
                .sorted(Comparator.comparing(PedidoDelGrupo::pedidoId))
                .map(p -> new PedidoItem(p.pedidoId(), p.cliente(), p.tipo(), p.estado(),
                        pesos(p.totalCentavos()), pesos(p.cobradoCentavos()), pesos(p.saldoCentavos()),
                        p.pedidoId().equals(g.pedidoTitularId())))
                .toList();
        String tipo = g.pedidos().stream().filter(PedidoDelGrupo::estaAbierto).map(PedidoDelGrupo::tipo)
                .findFirst().orElse(g.pedidos().isEmpty() ? null : g.pedidos().get(0).tipo());
        return new GrupoPedidosResponse(g.grupoId(), g.activo(), g.pedidoTitularId(),
                g.titular().map(PedidoDelGrupo::cliente).orElse(null), tipo,
                g.creado(), g.nota(),
                pesos(g.totalCentavos()), pesos(g.pagadoCentavos()), pesos(g.saldoCentavos()), items);
    }

    static double pesos(long centavos) {
        return centavos / 100.0;
    }
}
