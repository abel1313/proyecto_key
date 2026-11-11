package com.ventas.key.hexagonal.reportecategoria.dominio.modelo;

/**
 * Qué columnas lleva el reporte (los datos son los mismos; cambia lo que se muestra para que cada reporte sea claro):
 * COMPLETO = precios, ventas, por vender y flete, todo junto; VENTAS = lo vendido repartido por tipo de precio (venta, rebaja, campaña, otro).
 */
public enum TipoReporte {
    COMPLETO, VENTAS;

    public static TipoReporte de(String texto) {
        if (texto == null || texto.isBlank()) return COMPLETO;
        try {
            return valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de reporte no válido: " + texto + " (COMPLETO o VENTAS).");
        }
    }
}
