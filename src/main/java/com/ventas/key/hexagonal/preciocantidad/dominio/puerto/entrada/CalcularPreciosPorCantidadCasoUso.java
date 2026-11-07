package com.ventas.key.hexagonal.preciocantidad.dominio.puerto.entrada;

import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.LineaPorCantidad;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.PrecioDeGrupo;

import java.util.List;
import java.util.Map;

/** [Hexagonal: Driving Port] El precio de grupo que le toca a cada producto de una venta. Solo lee. */
public interface CalcularPreciosPorCantidadCasoUso {

    /** Por producto; lo que no aparece se cobra a su precio normal. */
    Map<Integer, PrecioDeGrupo> calcular(List<LineaPorCantidad> lineas);
}
