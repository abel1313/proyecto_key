package com.ventas.key.hexagonal.campana.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ventas.key.hexagonal.campana.dominio.modelo.Campana;

import java.time.LocalDate;
import java.util.List;

/** Lo que el front necesita de una campaña. {@code vigente} = encendida y dentro de sus fechas hoy. */
public record CampanaResponse(Integer id, String nombre,
                              @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fechaInicio,
                              @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fechaFin,
                              boolean activa, boolean vigente, double metaMonto, List<Producto> productos) {

    public record Producto(int productoId, String nombre, String codigoBarras, double precio, Double precioVenta, Double precioRebaja, Integer stock) {
    }

    public static CampanaResponse de(Campana c, LocalDate hoy) {
        return new CampanaResponse(c.id(), c.nombre(), c.fechaInicio(), c.fechaFin(), c.activa(), c.vigenteEn(hoy),
                c.metaMonto().doubleValue(),
                c.precios().stream().map(p -> new Producto(p.productoId(), p.nombre(), p.codigoBarras(), p.precio().doubleValue(), p.precioVenta(), p.precioRebaja(), p.stock())).toList());
    }
}
