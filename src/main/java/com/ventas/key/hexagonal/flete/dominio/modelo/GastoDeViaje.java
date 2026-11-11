package com.ventas.key.hexagonal.flete.dominio.modelo;

import com.ventas.key.hexagonal.flete.dominio.excepcion.ViajeInvalidoException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * [Hexagonal: Domain] Un gasto extra del viaje (gasolina, comida, bolsas, etiquetas...). Es opcional; {@code cuentaParaCubrir} dice si se suma al costo
 * que las piezas del viaje deben recuperar junto con el flete.
 */
public record GastoDeViaje(String concepto, BigDecimal monto, boolean cuentaParaCubrir) {

    public GastoDeViaje {
        concepto = concepto == null ? "" : concepto.trim();
        if (concepto.isEmpty()) {
            throw new ViajeInvalidoException("Falta el concepto de uno de los gastos del viaje.");
        }
        if (concepto.length() > 100) {
            throw new ViajeInvalidoException("El concepto del gasto «" + concepto.substring(0, 20) + "…» es demasiado largo (máximo 100 letras).");
        }
        monto = monto == null ? null : monto.setScale(2, RoundingMode.HALF_UP);
        if (monto == null || monto.signum() <= 0) {
            throw new ViajeInvalidoException("El monto del gasto «" + concepto + "» debe ser mayor a cero.");
        }
        if (monto.compareTo(ViajeDeCompra.MONTO_MAXIMO) > 0) {
            throw new ViajeInvalidoException("El monto del gasto «" + concepto + "» es demasiado grande.");
        }
    }
}
