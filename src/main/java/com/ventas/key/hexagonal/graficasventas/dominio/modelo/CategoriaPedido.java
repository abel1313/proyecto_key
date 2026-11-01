package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

/**
 * En qué punto está un pedido, en palabras del dueño. {@code activo} = sigue abierto (se cuenta siempre); los que ya terminaron
 * (Entregado, Cancelado) solo se cuentan dentro del periodo pedido.
 */
public enum CategoriaPedido {
    PENDIENTE("Pendiente de pago", true),
    APARTADO("Apartado", true),
    IR_PAGANDO("Ir pagando", true),
    PAGADO_POR_ENTREGAR("Pagado, falta entregar", true),
    ENTREGADO("Entregado", false),
    CANCELADO("Cancelado", false);

    private final String etiqueta;
    private final boolean activo;

    CategoriaPedido(String etiqueta, boolean activo) {
        this.etiqueta = etiqueta;
        this.activo = activo;
    }

    public String etiqueta() { return etiqueta; }
    public boolean activo() { return activo; }
}
