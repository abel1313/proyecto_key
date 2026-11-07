package com.ventas.key.hexagonal.preciocantidad.infraestructura.entrada.rest.dto;

import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.GrupoPrecioCantidad;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

/** Lo que el front necesita de un grupo. {@code vigente} = activo y dentro de sus fechas hoy. */
public record GrupoPrecioCantidadResponse(Integer id, String nombre, int minimoPiezas,
                                          @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fechaInicio,
                                          @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fechaFin,
                                          boolean activo, boolean vigente, List<Producto> productos) {

    public record Producto(int productoId, String nombre, double precioPromocion) {
    }

    public static GrupoPrecioCantidadResponse de(GrupoPrecioCantidad g, LocalDate hoy) {
        return new GrupoPrecioCantidadResponse(g.id(), g.nombre(), g.minimoPiezas(), g.fechaInicio(), g.fechaFin(), g.activo(),
                g.vigenteEn(hoy),
                g.miembros().stream().map(m -> new Producto(m.productoId(), m.nombre(), m.precioPromocion().doubleValue())).toList());
    }
}
