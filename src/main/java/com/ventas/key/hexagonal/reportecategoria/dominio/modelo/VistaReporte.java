package com.ventas.key.hexagonal.reportecategoria.dominio.modelo;

/** Qué productos entran al reporte: los que ya se vendieron en las fechas, los que no, o todos. */
public enum VistaReporte {
    VENDIDOS, NO_VENDIDOS, TODOS;

    public static VistaReporte de(String texto) {
        if (texto == null || texto.isBlank()) return TODOS;
        try {
            return valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Vista de reporte no válida: " + texto + " (VENDIDOS, NO_VENDIDOS o TODOS).");
        }
    }
}
