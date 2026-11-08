package com.ventas.key.hexagonal.campana.dominio.modelo;

import com.ventas.key.hexagonal.campana.dominio.excepcion.CampanaInvalidaException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * [Hexagonal: Domain] [Clean: Entity] Java puro.
 *
 * <p>Una campaña de juguetes (diseño en {@code PLAN_CAMPANAS_Y_PRECIOS.md}): un evento con nombre y fechas, una meta de monto y los productos
 * con su precio de campaña. Solo una campaña puede estar activa a la vez (lo cuida quien guarda). Está <b>vigente</b> cuando está encendida y hoy
 * cae dentro de sus fechas (los dos extremos incluidos): solo entonces aparece algo en Venta directa.
 *
 * @param id        {@code null} mientras no se guarda
 * @param metaMonto lo que tiene que juntar una persona comprando en el evento (sin contar los juguetes de la campaña) para ser recurrente; 0 = sin meta
 */
public record Campana(Integer id, String nombre, LocalDate fechaInicio, LocalDate fechaFin, boolean activa, BigDecimal metaMonto,
                      List<PrecioDeCampana> precios) {

    public Campana {
        if (nombre == null || nombre.isBlank()) {
            throw new CampanaInvalidaException("La campaña necesita un nombre.");
        }
        nombre = nombre.trim();
        if (fechaInicio == null || fechaFin == null) {
            throw new CampanaInvalidaException("La campaña necesita fecha de inicio y fecha de fin.");
        }
        if (fechaInicio.isAfter(fechaFin)) {
            throw new CampanaInvalidaException("La fecha de inicio no puede ser después de la fecha de fin.");
        }
        if (metaMonto == null) {
            metaMonto = BigDecimal.ZERO;
        }
        if (metaMonto.signum() < 0) {
            throw new CampanaInvalidaException("La meta de monto no puede ser negativa.");
        }
        metaMonto = metaMonto.setScale(2, java.math.RoundingMode.HALF_UP);
        // Puede quedar sin productos si el catálogo los borró; para guardarla con cambios, el caso de uso exige al menos uno.
        precios = precios == null ? List.of() : precios;
        Set<Integer> vistos = new HashSet<>();
        for (PrecioDeCampana p : precios) {
            if (!vistos.add(p.productoId())) {
                throw new CampanaInvalidaException("El producto " + (p.nombre() == null ? p.productoId() : "«" + p.nombre() + "»") + " está repetido en la campaña.");
            }
        }
        precios = List.copyOf(precios);
    }

    /** ¿Aparece hoy en Venta directa? Encendida y dentro de sus fechas. */
    public boolean vigenteEn(LocalDate hoy) {
        return activa && !hoy.isBefore(fechaInicio) && !hoy.isAfter(fechaFin);
    }

    public Optional<PrecioDeCampana> precioDe(int productoId) {
        return precios.stream().filter(p -> p.productoId() == productoId).findFirst();
    }
}
