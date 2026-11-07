package com.ventas.key.hexagonal.preciocantidad.infraestructura.entrada.rest.dto;

import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.LineaPorCantidad;

import java.util.List;

/** Lo que lleva la venta o el carrito: producto y piezas. Solo para calcular, no guarda nada. */
public record PrecioPorCantidadRequest(List<Linea> lineas) {

    public record Linea(Integer productoId, Integer cantidad) {
    }

    public List<LineaPorCantidad> aDominio() {
        if (lineas == null) {
            return List.of();
        }
        return lineas.stream()
                .filter(l -> l.productoId() != null && l.cantidad() != null && l.cantidad() > 0)
                .map(l -> new LineaPorCantidad(l.productoId(), l.cantidad()))
                .toList();
    }
}
