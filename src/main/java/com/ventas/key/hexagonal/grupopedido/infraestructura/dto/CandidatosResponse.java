package com.ventas.key.hexagonal.grupopedido.infraestructura.dto;

import com.ventas.key.hexagonal.grupopedido.dominio.modelo.PedidoDelGrupo;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.PedidosDelGrupoPort;

import java.time.LocalDateTime;
import java.util.List;

/** {@code GET /v1/grupos-pedido/candidatos}: una pagina del buscador de "Unir pedidos". */
public record CandidatosResponse(List<Candidato> pedidos, int pagina, boolean hayMas) {

    public record Candidato(Integer pedidoId, String cliente, String tipoPedido, String estadoPedido,
                            double total, double pagado, double saldo, LocalDateTime fecha) {
    }

    public static CandidatosResponse de(PedidosDelGrupoPort.PaginaDePedidos pagina, int numero) {
        return new CandidatosResponse(pagina.pedidos().stream().map(CandidatosResponse::candidato).toList(),
                numero, pagina.hayMas());
    }

    private static Candidato candidato(PedidoDelGrupo p) {
        return new Candidato(p.pedidoId(), p.cliente(), p.tipo(), p.estado(),
                p.totalCentavos() / 100.0, p.cobradoCentavos() / 100.0, p.saldoCentavos() / 100.0, p.registro());
    }
}
