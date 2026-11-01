package com.ventas.key.hexagonal.graficasventas.dominio.puerto.entrada;

import com.ventas.key.hexagonal.graficasventas.dominio.modelo.PuntoMensual;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.VentasPorFormaDePago;

import java.time.LocalDate;
import java.util.List;

/** [Hexagonal: Driving Port] [Clean: Use Case Input Boundary] */
public interface ConsultarGraficasCasoUso {

    /** Cómo te pagaron entre dos fechas (sin fechas: del día 1 del mes actual a hoy). Las ventas devueltas no cuentan. */
    VentasPorFormaDePago ventasPorFormaDePago(LocalDate desde, LocalDate hasta);

    /**
     * Ventas, ganancia, gastos y ganancia neta de los últimos {@code meses} meses, el actual incluido, del más viejo al más
     * nuevo. Siempre vienen todos los meses seguidos: uno sin movimiento sale en cero.
     */
    List<PuntoMensual> ventasYGastosPorMes(int meses);
}
