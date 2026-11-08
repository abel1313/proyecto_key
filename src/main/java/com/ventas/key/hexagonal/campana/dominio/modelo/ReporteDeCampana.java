package com.ventas.key.hexagonal.campana.dominio.modelo;

import java.math.BigDecimal;
import java.util.List;

/**
 * [Hexagonal: Domain] Cómo va una campaña: cuánto ha juntado cada persona con código y quién ya llegó a la meta. {@code acumulado} es lo que
 * suma hacia la meta (compras de Contado sin los juguetes de la campaña).
 */
public record ReporteDeCampana(int campanaId, String campana, BigDecimal meta, List<Fila> personas) {

    public record Fila(int personaId, String uuid, String nombre, String correo, String telefono, BigDecimal acumulado, long pedidos,
                       boolean llegoALaMeta) {
    }

    public ReporteDeCampana {
        personas = personas == null ? List.of() : List.copyOf(personas);
    }

    public int llegaronALaMeta() {
        return (int) personas.stream().filter(Fila::llegoALaMeta).count();
    }

    public BigDecimal totalAcumulado() {
        return personas.stream().map(Fila::acumulado).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
