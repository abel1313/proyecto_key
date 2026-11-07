package com.ventas.key.hexagonal.preciocantidad.aplicacion.servicio;

import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.GrupoPrecioCantidad;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.LineaPorCantidad;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.PrecioDeGrupo;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.ReglasPrecioCantidad;
import com.ventas.key.hexagonal.preciocantidad.dominio.puerto.entrada.CalcularPreciosPorCantidadCasoUso;
import com.ventas.key.hexagonal.preciocantidad.dominio.puerto.salida.GruposDePrecioPort;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** [Clean: Use Case] Orquesta: lee los grupos que importan y le pide al dominio el cálculo. Java puro (sin Spring). */
public class CalcularPreciosPorCantidadService implements CalcularPreciosPorCantidadCasoUso {

    private final GruposDePrecioPort grupos;
    private final Clock reloj;

    public CalcularPreciosPorCantidadService(GruposDePrecioPort grupos, Clock reloj) {
        this.grupos = grupos;
        this.reloj = reloj;
    }

    @Override
    public Map<Integer, PrecioDeGrupo> calcular(List<LineaPorCantidad> lineas) {
        if (lineas == null || lineas.isEmpty()) {
            return Map.of();
        }
        Set<Integer> productos = new HashSet<>();
        lineas.forEach(l -> productos.add(l.productoId()));
        List<GrupoPrecioCantidad> candidatos = grupos.activosConProductos(productos);
        return ReglasPrecioCantidad.calcular(candidatos, lineas, LocalDate.now(reloj));
    }
}
