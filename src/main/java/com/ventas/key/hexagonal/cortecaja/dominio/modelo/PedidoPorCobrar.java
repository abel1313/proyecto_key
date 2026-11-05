package com.ventas.key.hexagonal.cortecaja.dominio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * [Hexagonal: Domain] [Clean: Entity] Java puro.
 *
 * <p>Un pedido que todavía no se entrega y del que aún se debe dinero. {@code zonaId} y {@code zona} son {@code null} cuando el pedido
 * no tiene zona de entrega (se recoge en la tienda). {@code articulos} es la suma de las piezas del pedido y {@code pagos} cuántos abonos lleva.
 * {@code montos} (costo y ganancia) es solo para el administrador.
 */
public record PedidoPorCobrar(
        int pedidoId,
        String cliente,
        int articulos,
        BigDecimal total,
        BigDecimal pagado,
        int pagos,
        LocalDate fechaPedido,
        String estado,
        boolean entregado,
        Integer zonaId,
        String zona,
        MontosDeGanancia montos) {

    /** Menos que esto se considera pagado (centavos de redondeo). */
    public static final BigDecimal TOLERANCIA = new BigDecimal("0.005");

    public PedidoPorCobrar {
        total = dinero(total);
        pagado = dinero(pagado);
        montos = montos == null ? MontosDeGanancia.CERO : montos;
    }

    /** ¿Ya dio algo? Con un solo pago se le llama anticipo. */
    public boolean dioAnticipo() {
        return pagos == 1;
    }

    /** Lo que falta por cobrar de este pedido: total menos lo abonado, nunca negativo. */
    public BigDecimal saldo() {
        BigDecimal resta = total.subtract(pagado);
        return resta.signum() < 0 ? BigDecimal.ZERO.setScale(2) : resta.setScale(2, RoundingMode.HALF_UP);
    }

    /** ¿Todavía se debe algo? Un pedido ya pagado no tiene nada que cobrar. */
    public boolean tieneSaldo() {
        return saldo().compareTo(TOLERANCIA) > 0;
    }

    public boolean tieneZona() {
        return zonaId != null;
    }

    private static BigDecimal dinero(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }
}
