package com.ventas.key.hexagonal.preciocantidad.dominio.modelo;

import com.ventas.key.hexagonal.preciocantidad.dominio.excepcion.GrupoDePrecioInvalidoException;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * [Hexagonal: Domain] [Clean: Entity] Java puro.
 *
 * <p>Un grupo de precio por cantidad (reglas G1 a G4 de {@code PLAN_CAMPANAS_Y_PRECIOS.md}): un nombre, el mínimo de piezas, fechas opcionales,
 * un interruptor y los productos que lo forman, cada uno con su precio de promoción. Un producto está en un solo grupo (G2): eso lo cuida
 * quien guarda, porque depende de los demás grupos.
 *
 * @param id          {@code null} mientras no se guarda
 * @param fechaInicio primer día en que vale; {@code null} = desde siempre
 * @param fechaFin    último día en que vale (incluido); {@code null} = sin fin
 */
public record GrupoPrecioCantidad(Integer id, String nombre, int minimoPiezas, LocalDate fechaInicio, LocalDate fechaFin, boolean activo,
                                  List<MiembroDeGrupo> miembros) {

    /** Con menos de 2 piezas no habría nada que juntar. */
    public static final int MINIMO_PERMITIDO = 2;

    public GrupoPrecioCantidad {
        if (nombre == null || nombre.isBlank()) {
            throw new GrupoDePrecioInvalidoException("El grupo necesita un nombre.");
        }
        nombre = nombre.trim();
        if (minimoPiezas < MINIMO_PERMITIDO) {
            throw new GrupoDePrecioInvalidoException("El mínimo de piezas debe ser " + MINIMO_PERMITIDO + " o más.");
        }
        if (fechaInicio != null && fechaFin != null && fechaInicio.isAfter(fechaFin)) {
            throw new GrupoDePrecioInvalidoException("La fecha de inicio no puede ser después de la fecha de fin.");
        }
        // Puede quedar sin productos si el catálogo los borró; para guardarlo con cambios, el caso de uso exige al menos uno.
        miembros = miembros == null ? List.of() : miembros;
        Set<Integer> vistos = new HashSet<>();
        for (MiembroDeGrupo m : miembros) {
            if (!vistos.add(m.productoId())) {
                throw new GrupoDePrecioInvalidoException("El producto " + (m.nombre() == null ? m.productoId() : m.nombre())
                        + " está repetido en el grupo.");
            }
        }
        miembros = List.copyOf(miembros);
    }

    /** ¿Vale hoy? Activo y dentro de sus fechas (los dos extremos incluidos). */
    public boolean vigenteEn(LocalDate hoy) {
        return activo && (fechaInicio == null || !hoy.isBefore(fechaInicio)) && (fechaFin == null || !hoy.isAfter(fechaFin));
    }

    public Optional<MiembroDeGrupo> miembro(int productoId) {
        return miembros.stream().filter(m -> m.productoId() == productoId).findFirst();
    }
}
