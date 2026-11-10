package com.ventas.key.hexagonal.campana.dominio.modelo;

import java.math.BigDecimal;

/** [Hexagonal: Domain] Una persona y lo que ha juntado en una campaña (lo que devuelve el puerto de personas para el reporte). */
public record ResumenDePersona(PersonaCampana persona, BigDecimal acumulado, long pedidos) {
}
