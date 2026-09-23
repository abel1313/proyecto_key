package com.ventas.key.hexagonal.grupopedido.infraestructura.dto;

import com.ventas.key.hexagonal.grupopedido.dominio.puerto.entrada.UnirPedidosCasoUso;

/**
 * @param grupo        el grupo separado (inactivo) con como quedo cada pedido: pagado, saldo, estado
 * @param grupoNuevoId el grupo en el que siguen unidos los demas, o null si ya no queda grupo
 */
public record SeparacionResponse(GrupoPedidosResponse grupo, Integer grupoNuevoId) {

    public static SeparacionResponse de(UnirPedidosCasoUso.ResultadoSeparacion r) {
        return new SeparacionResponse(GrupoPedidosResponse.de(r.grupo()), r.grupoNuevo());
    }
}
