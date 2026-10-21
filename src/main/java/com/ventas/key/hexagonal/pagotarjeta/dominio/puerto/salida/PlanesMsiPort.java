package com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.PlanMsi;

import java.math.BigDecimal;
import java.util.List;

/**
 * La configuracion de meses sin intereses (M3 y M4): que planes estan activos y desde que monto.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 */
public interface PlanesMsiPort {

    List<PlanMsi> planes();

    BigDecimal minimoParaMsi();
}
