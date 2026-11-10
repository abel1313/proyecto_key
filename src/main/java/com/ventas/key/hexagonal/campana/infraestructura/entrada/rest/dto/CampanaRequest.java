package com.ventas.key.hexagonal.campana.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ventas.key.hexagonal.campana.dominio.modelo.Campana;
import com.ventas.key.hexagonal.campana.dominio.modelo.PrecioDeCampana;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** El JSON que manda el front para crear o cambiar una campaña. Si está encendida o no se cambia con activar / apagar, no aquí. */
public record CampanaRequest(String nombre,
                             @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fechaInicio,
                             @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fechaFin,
                             BigDecimal metaMonto, List<Precio> precios) {

    public record Precio(Integer productoId, BigDecimal precio) {
    }

    public Campana aDominio() {
        List<PrecioDeCampana> lista = precios == null ? List.of() : precios.stream()
                .map(p -> new PrecioDeCampana(p.productoId() == null ? 0 : p.productoId(), null, p.precio()))
                .toList();
        return new Campana(null, nombre, fechaInicio, fechaFin, false, metaMonto == null ? BigDecimal.ZERO : metaMonto, lista);
    }
}
