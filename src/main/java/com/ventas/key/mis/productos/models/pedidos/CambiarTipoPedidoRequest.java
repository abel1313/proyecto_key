package com.ventas.key.mis.productos.models.pedidos;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * Cambiar la forma de cobro de un pedido ya creado, con el cobro que eso implique.
 *
 * <p>Nace de un caso real (2026-09-22): se aparto un pedido, al ir a entregarlo el cliente
 * decidio pagarlo completo, y como no habia forma de cambiarlo hubo que dejarlo registrado como
 * apartado. La alternativa era cancelar y rehacer el pedido entero, que ademas devolvia y volvia
 * a descontar el stock.
 *
 * @param tipoPedido  a que pasa: NORMAL (contado), APARTADO o FIADO
 * @param monto       lo que se cobra en este momento; null o 0 si solo se cambia el tipo sin cobrar
 * @param metodoPago  EFECTIVO o TRANSFERENCIA (ver AbonoServiceImpl: el credito no toma TARJETA)
 * @param montoDado   solo para EFECTIVO, para calcular el cambio
 * @param nota        que paso, escrito por quien hace el cambio. Es lo unico que queda para
 *                    entender manana por que este pedido cambio de forma de cobro
 * @param usuarioId   quien lo hizo
 */
@Data
public class CambiarTipoPedidoRequest {

    private String tipoPedido;
    // El front lo manda como montoCobrado/descripcion: sin el alias el cobro se ignoraba en silencio.
    @JsonAlias("montoCobrado")
    private Double monto;
    private String metodoPago = "EFECTIVO";
    private Double montoDado;
    @JsonAlias("descripcion")
    private String nota;
    private Integer usuarioId;

    public boolean traeCobro() {
        return monto != null && monto > 0;
    }
}
