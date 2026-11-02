package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

/**
 * Este mes hasta hoy contra el mes anterior hasta el mismo día del mes (si el mes anterior es más corto, hasta su último día).
 * Se comparan tramos iguales: el 10 del mes no se compara contra el mes anterior completo.
 *
 * @param hastaElDia día del mes hasta el que se comparan los dos tramos (el de hoy)
 */
public record ComparativoDelMes(int hastaElDia, PeriodoDeVentas actual, PeriodoDeVentas anterior) {

    /** Cambio porcentual de {@code ahora} contra {@code antes}; {@code null} si antes era cero (no se puede comparar). */
    public static Double cambio(double ahora, double antes) {
        if (Math.abs(antes) < 0.005) {
            return null;
        }
        return Math.round((ahora - antes) * 1000.0 / Math.abs(antes)) / 10.0;
    }
}
