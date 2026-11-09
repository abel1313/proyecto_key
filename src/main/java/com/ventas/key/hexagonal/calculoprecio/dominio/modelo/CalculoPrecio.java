package com.ventas.key.hexagonal.calculoprecio.dominio.modelo;

import com.ventas.key.hexagonal.calculoprecio.dominio.excepcion.CalculoPrecioInvalidoException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * [Clean: Entity] Lo que el dueño calculó para ponerle precio a un producto: lo que le costó (compra + transporte + otros), la comisión de la
 * terminal que eligió, el margen, el precio que calculó la calculadora y el que de verdad cobra (pueden ser distintos: calculó 120 y cobra 150).
 * Es un registro histórico: cada «Usar este precio» guarda uno nuevo; el último es el vigente. Todo es por UNA pieza.
 * Reglas: R1 costos no negativos y compra mayor a 0; R2 comisión de 0 a 50 %; R3 margen de 0 a menos de 100 %; R4 los dos precios mayores a 0;
 * R5 ganancia y margen real se calculan aquí, no los manda el front.
 */
public record CalculoPrecio(Integer id, int productoId, BigDecimal costoCompra, BigDecimal transporte, BigDecimal otros,
                            BigDecimal comisionPct, String comisionDescripcion, BigDecimal margenPct,
                            BigDecimal precioCalculado, BigDecimal precioCobrado, boolean actualizoPrecioCompra,
                            String nota, String usuario, LocalDateTime fecha) {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    public static final int MAX_NOTA = 300;

    public CalculoPrecio {
        if (productoId <= 0) throw new CalculoPrecioInvalidoException("Falta el producto del cálculo.");
        costoCompra = dinero(costoCompra);
        transporte = dinero(transporte);
        otros = dinero(otros);
        comisionPct = comisionPct == null ? BigDecimal.ZERO : comisionPct;
        margenPct = margenPct == null ? BigDecimal.ZERO : margenPct;
        if (costoCompra.signum() <= 0) throw new CalculoPrecioInvalidoException("El costo de compra tiene que ser mayor a 0.");
        if (transporte.signum() < 0 || otros.signum() < 0) throw new CalculoPrecioInvalidoException("El transporte y los otros gastos no pueden ser negativos.");
        if (comisionPct.signum() < 0 || comisionPct.compareTo(BigDecimal.valueOf(50)) > 0) throw new CalculoPrecioInvalidoException("La comisión de tarjeta tiene que estar entre 0 y 50 %.");
        if (margenPct.signum() < 0 || margenPct.compareTo(CIEN) >= 0) throw new CalculoPrecioInvalidoException("El margen tiene que estar entre 0 y menos de 100 %.");
        if (precioCalculado == null || precioCalculado.signum() <= 0) throw new CalculoPrecioInvalidoException("Falta el precio que calculó la calculadora.");
        if (precioCobrado == null || precioCobrado.signum() <= 0) throw new CalculoPrecioInvalidoException("Falta el precio que vas a cobrar.");
        precioCalculado = precioCalculado.setScale(2, RoundingMode.HALF_UP);
        precioCobrado = precioCobrado.setScale(2, RoundingMode.HALF_UP);
        nota = nota == null || nota.isBlank() ? null : nota.trim();
        if (nota != null && nota.length() > MAX_NOTA) throw new CalculoPrecioInvalidoException("La nota puede tener máximo " + MAX_NOTA + " letras.");
        comisionDescripcion = comisionDescripcion == null || comisionDescripcion.isBlank() ? null : comisionDescripcion.trim();
    }

    private static BigDecimal dinero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    /** Costo total real de una pieza: compra + transporte + otros. */
    public BigDecimal costoTotal() {
        return costoCompra.add(transporte).add(otros);
    }

    /** Lo que se queda la terminal de cada pieza al precio cobrado. */
    public BigDecimal comisionEnPesos() {
        return precioCobrado.multiply(comisionPct).divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    /** Ganancia limpia por pieza al precio cobrado (puede ser negativa). */
    public BigDecimal gananciaReal() {
        return precioCobrado.subtract(costoTotal()).subtract(comisionEnPesos());
    }

    /** Margen real (% del precio cobrado). */
    public BigDecimal margenReal() {
        return gananciaReal().multiply(CIEN).divide(precioCobrado, 2, RoundingMode.HALF_UP);
    }

    /** Lo mismo con el sello de quién y cuándo se registró. */
    public CalculoPrecio registradoPor(String quien, LocalDateTime cuando) {
        return new CalculoPrecio(id, productoId, costoCompra, transporte, otros, comisionPct, comisionDescripcion, margenPct,
                precioCalculado, precioCobrado, actualizoPrecioCompra, nota, quien, cuando);
    }
}
