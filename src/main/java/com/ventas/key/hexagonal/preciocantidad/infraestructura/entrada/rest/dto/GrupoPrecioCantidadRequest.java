package com.ventas.key.hexagonal.preciocantidad.infraestructura.entrada.rest.dto;

import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.GrupoPrecioCantidad;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.MiembroDeGrupo;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** El JSON que manda el front para crear o cambiar un grupo. */
public record GrupoPrecioCantidadRequest(String nombre, Integer minimoPiezas,
                                         @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fechaInicio,
                                         @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fechaFin,
                                         Boolean activo, List<Producto> productos) {

    public record Producto(Integer productoId, BigDecimal precioPromocion) {
    }

    public GrupoPrecioCantidad aDominio() {
        List<MiembroDeGrupo> miembros = productos == null ? List.of() : productos.stream()
                .map(p -> new MiembroDeGrupo(p.productoId() == null ? 0 : p.productoId(), null, p.precioPromocion()))
                .toList();
        return new GrupoPrecioCantidad(null, nombre, minimoPiezas == null ? 0 : minimoPiezas, fechaInicio, fechaFin,
                activo == null || activo, miembros);
    }
}
