package com.ventas.key.hexagonal.campana.infraestructura.entrada.rest.dto;

import com.ventas.key.hexagonal.campana.dominio.modelo.ReporteDeCampana;

import java.util.List;

/** El reporte de una campaña para la pantalla Catálogo → Campañas. */
public record ReporteDeCampanaResponse(int campanaId, String campana, double meta, int totalPersonas, int llegaronALaMeta,
                                       double totalAcumulado, List<Persona> personas) {

    public record Persona(int personaId, String codigo, String nombre, String correo, String telefono, double acumulado, long pedidos,
                          boolean llegoALaMeta) {
    }

    public static ReporteDeCampanaResponse de(ReporteDeCampana r) {
        return new ReporteDeCampanaResponse(r.campanaId(), r.campana(), r.meta().doubleValue(), r.personas().size(), r.llegaronALaMeta(),
                r.totalAcumulado().doubleValue(),
                r.personas().stream().map(f -> new Persona(f.personaId(), f.uuid(), f.nombre(), f.correo(), f.telefono(),
                        f.acumulado().doubleValue(), f.pedidos(), f.llegoALaMeta())).toList());
    }
}
