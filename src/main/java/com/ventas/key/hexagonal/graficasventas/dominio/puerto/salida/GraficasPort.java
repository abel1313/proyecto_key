package com.ventas.key.hexagonal.graficasventas.dominio.puerto.salida;

import com.ventas.key.hexagonal.graficasventas.dominio.modelo.FormaDePagoVentas;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.VentasDelMes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * De dónde salen los números de las gráficas. SOLO LECTURA. Las ventas con estado «Devuelta» no se cuentan.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 */
public interface GraficasPort {

    /** Ventas entre las dos fechas (ambas incluidas), por forma de pago. */
    List<FormaDePagoVentas> ventasPorFormaDePago(LocalDate desde, LocalDate hasta);

    /** Solo los meses en que hubo ventas. */
    Map<YearMonth, VentasDelMes> ventasPorMes(LocalDate desde, LocalDate hasta);

    /** Solo los meses en que hubo gastos. */
    Map<YearMonth, Double> gastosPorMes(LocalDate desde, LocalDate hasta);
}
