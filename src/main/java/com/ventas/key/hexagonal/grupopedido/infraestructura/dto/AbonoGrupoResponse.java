package com.ventas.key.hexagonal.grupopedido.infraestructura.dto;

import com.ventas.key.hexagonal.grupopedido.dominio.puerto.entrada.UnirPedidosCasoUso;

import java.util.List;

/** El grupo despues del abono, cuanto le toco a cada pedido y el cambio. */
public record AbonoGrupoResponse(GrupoPedidosResponse grupo, List<RepartoItem> repartos, double cambio) {

    /** @param liquida si con esta parte el pedido quedo pagado */
    public record RepartoItem(Integer pedidoId, double monto, boolean liquida) {
    }

    public static AbonoGrupoResponse de(UnirPedidosCasoUso.ResultadoAbono r) {
        return new AbonoGrupoResponse(
                GrupoPedidosResponse.de(r.grupo()),
                r.repartos().stream()
                        .map(x -> new RepartoItem(x.pedidoId(), GrupoPedidosResponse.pesos(x.montoCentavos()), x.liquida()))
                        .toList(),
                GrupoPedidosResponse.pesos(r.cambioCentavos()));
    }
}
