package com.ventas.key.hexagonal.reportecategoria.dominio.modelo;

import java.time.LocalDate;
import java.util.List;

/**
 * Lo que el administrador pide.
 * {@code categoria} vacía = todas (con o sin categoría); {@code sinCategoria} = solo los productos sin ninguna categoría («Otros»).
 * {@code productoIds} no vacía = solo esos productos (se ignoran categoría y «Otros»).
 */
public record FiltroReporte(LocalDate desde, LocalDate hasta, String categoria, boolean sinCategoria,
                            List<Integer> productoIds, VistaReporte vista, TipoReporte tipo) {

    public FiltroReporte {
        if (desde == null || hasta == null) throw new IllegalArgumentException("Falta la fecha de inicio o la de fin.");
        if (hasta.isBefore(desde)) throw new IllegalArgumentException("La fecha de fin no puede ser antes que la de inicio.");
        categoria = categoria == null || categoria.isBlank() ? null : categoria.trim().toUpperCase(java.util.Locale.ROOT);   // las categorías se guardan en MAYÚSCULAS
        productoIds = productoIds == null ? List.of() : List.copyOf(productoIds);
        vista = vista == null ? VistaReporte.TODOS : vista;
        tipo = tipo == null ? TipoReporte.COMPLETO : tipo;
        if (categoria != null && sinCategoria) throw new IllegalArgumentException("Elige una categoría o «Otros», no las dos.");
    }

    public boolean porProductos() {
        return !productoIds.isEmpty();
    }
}
