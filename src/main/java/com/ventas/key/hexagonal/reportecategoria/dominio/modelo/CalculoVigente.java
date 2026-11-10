package com.ventas.key.hexagonal.reportecategoria.dominio.modelo;

import java.math.BigDecimal;

/** El último cálculo de precio guardado de un producto: margen (%) que se calculó y flete (transporte por pieza) que llevaba. */
public record CalculoVigente(int productoId, BigDecimal margenPct, BigDecimal transporte) {
}
