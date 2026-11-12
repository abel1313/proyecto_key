package com.ventas.key.hexagonal.flete.infraestructura.entrada.rest.dto;

import com.ventas.key.hexagonal.flete.dominio.modelo.LigaManual;

import java.util.List;

/** Lo que manda el front para ligar ventas anteriores a un viaje: el producto y, por cada línea de venta, cuántas piezas salieron de ese viaje. */
public record LigarVentasRequest(Integer productoId, List<Liga> ligas) {

    public record Liga(Integer detalleVentaId, Integer cantidad) {
    }

    public List<LigaManual> aDominio() {
        return ligas == null ? List.of() : ligas.stream()
                .map(l -> new LigaManual(l.detalleVentaId() == null ? 0 : l.detalleVentaId(), l.cantidad() == null ? 0 : l.cantidad())).toList();
    }
}
