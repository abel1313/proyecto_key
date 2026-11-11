package com.ventas.key.hexagonal.flete.dominio.modelo;

import java.math.BigDecimal;

/** [Hexagonal: Domain] El producto de un viaje al que se van a ligar ventas: sus piezas restantes y el flete por pieza que se guarda en cada liga. */
public record RenglonParaLigar(int renglonId, String viaje, String producto, int restantes, BigDecimal fletePorPieza) {
}
