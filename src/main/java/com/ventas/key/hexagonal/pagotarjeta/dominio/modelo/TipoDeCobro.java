package com.ventas.key.hexagonal.pagotarjeta.dominio.modelo;

/**
 * La forma de cobro de un pedido, en palabras del dueño.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Solo {@link #CONTADO} (pedido normal sin pagar: en el local con terminal o desde la tienda en linea)
 * y {@link #PAGO_EN_LINEA} aceptan tarjeta (T3). Apartado es sin dinero e Ir pagando es con abonos en
 * efectivo o transferencia.
 */
public enum TipoDeCobro {
    CONTADO("Contado"),
    APARTADO("Apartado"),
    IR_PAGANDO("Ir pagando"),
    PAGO_EN_LINEA("Pago en línea");

    private final String etiqueta;

    TipoDeCobro(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Como lo dice el dueño; es lo que ven los mensajes (nunca el nombre del enum). */
    public String etiqueta() {
        return etiqueta;
    }

    /** T3: ni Apartado ni Ir pagando se pagan con tarjeta. */
    public boolean admiteTarjeta() {
        return this == CONTADO || this == PAGO_EN_LINEA;
    }

    /** M5: Ir pagando nunca tiene meses sin intereses; Apartado todavia no (es 🔮). */
    public boolean admiteMsi() {
        return admiteTarjeta();
    }
}
