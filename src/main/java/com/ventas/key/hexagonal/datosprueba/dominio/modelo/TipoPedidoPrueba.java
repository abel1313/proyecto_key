package com.ventas.key.hexagonal.datosprueba.dominio.modelo;

import java.util.Random;

/**
 * La mezcla de pedidos de prueba (R9). Los porcentajes suman 100.
 *
 * <p>Ninguno rompe las reglas de cobro (skill reglas-pedidos): un Apartado nace sin dinero y solo se
 * paga completo; un Ir pagando nace con enganche.
 */
public enum TipoPedidoPrueba {
    /** Venta de contado en efectivo: queda Entregado y con su venta. */
    CONTADO(40),
    /** Apartado sin dinero: como los que llegan por Facebook o un live. */
    APARTADO(20),
    /** Apartado que el cliente ya fue a recoger y pago completo: queda Pagado. */
    APARTADO_PAGADO(10),
    /** Ir pagando: enganche del 20 al 50 %, de 0 a 2 abonos mas; 1 de cada 4 se termina de pagar. */
    IR_PAGANDO(30);

    private final int porcentaje;

    TipoPedidoPrueba(int porcentaje) {
        this.porcentaje = porcentaje;
    }

    public static TipoPedidoPrueba elegir(Random rnd) {
        int tiro = rnd.nextInt(100);
        int acumulado = 0;
        for (TipoPedidoPrueba t : values()) {
            acumulado += t.porcentaje;
            if (tiro < acumulado) {
                return t;
            }
        }
        return IR_PAGANDO;
    }

    /** El tipo de pedido como lo guarda la venta directa (FIADO = Ir pagando en pantalla). */
    public String tipoEnVenta() {
        return switch (this) {
            case CONTADO -> "NORMAL";
            case APARTADO, APARTADO_PAGADO -> "APARTADO";
            case IR_PAGANDO -> "FIADO";
        };
    }
}
