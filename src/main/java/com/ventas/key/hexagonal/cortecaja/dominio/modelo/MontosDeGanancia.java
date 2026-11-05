package com.ventas.key.hexagonal.cortecaja.dominio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * [Hexagonal: Domain] [Clean: Entity] Java puro. SOLO PARA EL ADMINISTRADOR.
 *
 * <p>Lo que cuesta y lo que deja un pedido (o una suma de pedidos), en tres precios:
 * <ul>
 *   <li>{@code costo}: lo que costó la mercancía (costo actual del producto × cantidad);</li>
 *   <li>{@code ventaNormal}: los artículos al precio de venta normal;</li>
 *   <li>{@code conDescuento}: los artículos con su descuento, si lo tienen (si no, igual al precio normal).</li>
 * </ul>
 * {@code hayDescuento} dice si al menos un artículo tiene descuento, para no mostrar una columna repetida.
 */
public record MontosDeGanancia(BigDecimal costo, BigDecimal ventaNormal, BigDecimal conDescuento, boolean hayDescuento) {

    public static final MontosDeGanancia CERO =
            new MontosDeGanancia(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false);

    public MontosDeGanancia {
        costo = dinero(costo);
        ventaNormal = dinero(ventaNormal);
        conDescuento = dinero(conDescuento);
    }

    /** Lo que deja a precio normal: venta menos costo (puede ser negativo si se vende bajo costo). */
    public BigDecimal gananciaNormal() {
        return ventaNormal.subtract(costo);
    }

    /** Lo que deja con el descuento aplicado. */
    public BigDecimal gananciaConDescuento() {
        return conDescuento.subtract(costo);
    }

    public MontosDeGanancia mas(MontosDeGanancia otro) {
        return new MontosDeGanancia(costo.add(otro.costo), ventaNormal.add(otro.ventaNormal),
                conDescuento.add(otro.conDescuento), hayDescuento || otro.hayDescuento);
    }

    private static BigDecimal dinero(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }
}
