package com.ventas.key.hexagonal.grupopedido.infraestructura.dto;

import com.ventas.key.hexagonal.grupopedido.dominio.puerto.entrada.CambiarFormaDeCobroGrupoCasoUso;

import java.util.List;

/**
 * El grupo despues de cambiar su forma de cobro.
 *
 * @param tipoAnterior     la forma de cobro que tenia ({@code NORMAL}, {@code APARTADO}, {@code FIADO})
 * @param pedidosCambiados los pedidos que cambiaron
 * @param abono            el abono que dio el cliente en el mismo paso, o null
 */
public record CambioFormaCobroGrupoResponse(GrupoPedidosResponse grupo, String tipoAnterior,
                                            List<Integer> pedidosCambiados, AbonoGrupoResponse abono) {

    public static CambioFormaCobroGrupoResponse de(CambiarFormaDeCobroGrupoCasoUso.Resultado r) {
        return new CambioFormaCobroGrupoResponse(
                GrupoPedidosResponse.de(r.grupo()),
                r.tipoAnterior(),
                r.pedidosCambiados(),
                r.abono() == null ? null : AbonoGrupoResponse.de(r.abono()));
    }
}
