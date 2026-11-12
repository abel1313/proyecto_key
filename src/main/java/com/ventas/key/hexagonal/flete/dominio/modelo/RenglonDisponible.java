package com.ventas.key.hexagonal.flete.dominio.modelo;

import java.math.BigDecimal;

/** [Hexagonal: Domain] Un renglón de un viaje con las piezas que aún le quedan por vender, en el orden en que se venden (el viaje más viejo primero). */
public record RenglonDisponible(int renglonId, int restantes, BigDecimal fletePorPieza) {
}
