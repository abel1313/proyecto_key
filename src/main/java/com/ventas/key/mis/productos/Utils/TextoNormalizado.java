package com.ventas.key.mis.productos.Utils;

import java.util.Locale;

/**
 * Formato de los textos que el dueño quiere siempre iguales (2026-10-14): marca, color, talla y
 * categorías en MAYÚSCULAS; lugares de entrega en estilo oración ("Colonia centro").
 * Se aplica en el @PrePersist/@PreUpdate de cada entidad, así cubre cualquier camino que guarde
 * (pantallas, carga rápida, Excel).
 */
public final class TextoNormalizado {

    private static final Locale MX = new Locale("es", "MX");

    private TextoNormalizado() {}

    /** Quita espacios de más y pasa a MAYÚSCULAS. null se queda null. */
    public static String mayusculas(String texto) {
        if (texto == null) return null;
        return texto.trim().replaceAll("\\s+", " ").toUpperCase(MX);
    }

    /** Solo la primera letra en mayúscula y el resto en minúscula. null se queda null. */
    public static String oracion(String texto) {
        if (texto == null) return null;
        String limpio = texto.trim().replaceAll("\\s+", " ");
        if (limpio.isEmpty()) return limpio;
        return limpio.substring(0, 1).toUpperCase(MX) + limpio.substring(1).toLowerCase(MX);
    }
}
