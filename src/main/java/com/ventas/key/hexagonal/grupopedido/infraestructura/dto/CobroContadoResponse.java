package com.ventas.key.hexagonal.grupopedido.infraestructura.dto;

import com.ventas.key.hexagonal.grupopedido.dominio.puerto.entrada.UnirPedidosCasoUso;

import java.util.List;

/** El grupo despues del cobro y los pedidos que se confirmaron. */
public record CobroContadoResponse(GrupoPedidosResponse grupo, List<Integer> pedidosCobrados) {

    public static CobroContadoResponse de(UnirPedidosCasoUso.ResultadoCobro r) {
        return new CobroContadoResponse(GrupoPedidosResponse.de(r.grupo()), r.pedidosCobrados());
    }
}
