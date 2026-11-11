package com.ventas.key.hexagonal.flete.dominio.puerto.entrada;

import com.ventas.key.hexagonal.flete.dominio.modelo.ReporteDeViaje;

/** [Hexagonal: Driving Port] Cómo va un viaje: flete recuperado, lo que falta, desde qué venta se pagó, lo vendido y lo que queda. Solo lectura. */
public interface ConsultarReporteDeViajeCasoUso {
    ReporteDeViaje reporte(int viajeId);
}
