package com.ventas.key.hexagonal.reportecategoria.infraestructura.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Lo que se pide. {@code categoria} vacía = todas; {@code sinCategoria} = «Otros»; {@code productoIds} = solo esos productos.
 * {@code vista}: VENDIDOS, NO_VENDIDOS o TODOS (por defecto). {@code formato}: XLSX (por defecto) o PDF.
 */
public record GenerarReporteRequest(LocalDate desde, LocalDate hasta, String categoria, Boolean sinCategoria, List<Integer> productoIds,
                                    String vista, String formato) {
}
