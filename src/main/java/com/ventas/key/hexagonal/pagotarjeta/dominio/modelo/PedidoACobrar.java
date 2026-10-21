package com.ventas.key.hexagonal.pagotarjeta.dominio.modelo;

import java.math.BigDecimal;
import java.util.List;

/**
 * El pedido visto desde el cobro: de cuanto es, en que forma de cobro va y que articulos lleva.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * @param clienteId dueño del pedido (null si lo hizo un cliente sin registro); el cobro lo copia
 *                  para que la capa web pueda comprobar que quien consulta, cancela o devuelve es
 *                  el dueño o un administrador
 * @param total     T2: lo calcula el back; el front nunca manda el monto a cobrar
 * @param pagado    true si el pedido ya esta Pagado (no se vuelve a cobrar)
 * @param pagadoPorIntentoId el cobro que lo dejo pagado (null si se pago por otra via o no esta pagado);
 *                  permite distinguir "ya lo pague yo" (idempotencia) de "otro cobro lo pago" (doble pago)
 */
public record PedidoACobrar(
        Integer pedidoId,
        Integer clienteId,
        TipoDeCobro tipo,
        BigDecimal total,
        boolean pagado,
        Long pagadoPorIntentoId,
        List<ArticuloACobrar> articulos) {
}
