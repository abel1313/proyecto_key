package com.ventas.key.hexagonal.busquedapedido.dominio.modelo;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Las formas de cobro con el nombre del dueño. {@code codigo} es lo que guarda
 * {@code pedidos.tipo_pedido}: Contado = {@code NORMAL}, Ir pagando = {@code FIADO}.
 *
 * <p>{@code PENDIENTE} (2026-10-07, pedido del dueño): el pedido que el cliente hizo desde su
 * cuenta y nadie ha cobrado ni pasado a Apartado / Ir pagando. Se guarda como {@code NORMAL} con
 * {@code estado_pedido = 'Pendiente'}; {@code CONTADO} ya no los incluye (son los contado que ya se
 * cobraron). Antes no habia forma de verlos aparte: salian como "Contado + Falta pagar".
 */
public enum FormaDeCobro {

    PENDIENTE("NORMAL"),
    CONTADO("NORMAL"),
    APARTADO("APARTADO"),
    IR_PAGANDO("FIADO");

    private final String codigo;

    FormaDeCobro(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
