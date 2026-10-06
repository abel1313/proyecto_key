package com.ventas.key.hexagonal.busquedapedido.dominio.modelo;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Las tres formas de cobro con el nombre del dueño. {@code codigo} es lo que guarda
 * {@code pedidos.tipo_pedido}: Contado = {@code NORMAL}, Ir pagando = {@code FIADO}.
 */
public enum FormaDeCobro {

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
