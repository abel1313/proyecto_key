package com.ventas.key.hexagonal.campana.aplicacion.servicio;

import com.ventas.key.hexagonal.campana.dominio.excepcion.CampanaInvalidaException;
import com.ventas.key.hexagonal.campana.dominio.modelo.Campana;
import com.ventas.key.hexagonal.campana.dominio.modelo.ReporteDeCampana;
import com.ventas.key.hexagonal.campana.dominio.modelo.ResumenDePersona;
import com.ventas.key.hexagonal.campana.dominio.puerto.entrada.ConsultarReporteDeCampanaCasoUso;
import com.ventas.key.hexagonal.campana.dominio.puerto.salida.CampanasPort;
import com.ventas.key.hexagonal.campana.dominio.puerto.salida.PersonasCampanaPort;

import java.util.Comparator;
import java.util.List;

/** [Clean: Use Case] Arma el reporte de una campaña. Sin las tablas de personas, el reporte sale vacío. Java puro. */
public class ConsultarReporteDeCampanaService implements ConsultarReporteDeCampanaCasoUso {

    private final CampanasPort campanas;
    private final PersonasCampanaPort personas;

    public ConsultarReporteDeCampanaService(CampanasPort campanas, PersonasCampanaPort personas) {
        this.campanas = campanas;
        this.personas = personas;
    }

    @Override
    public ReporteDeCampana reporte(int campanaId) {
        Campana c = campanas.buscar(campanaId).orElseThrow(() -> new CampanaInvalidaException("La campaña ya no existe."));
        List<ResumenDePersona> resumen = personas.disponible() ? personas.resumen(campanaId) : List.of();
        List<ReporteDeCampana.Fila> filas = resumen.stream()
                .sorted(Comparator.comparing(ResumenDePersona::acumulado).reversed())
                .map(r -> new ReporteDeCampana.Fila(r.persona().id(), r.persona().uuid(), r.persona().nombre(), r.persona().correo(),
                        r.persona().telefono(), r.acumulado(), r.pedidos(),
                        c.metaMonto().signum() > 0 && r.acumulado().compareTo(c.metaMonto()) >= 0))
                .toList();
        return new ReporteDeCampana(c.id(), c.nombre(), c.metaMonto(), filas);
    }
}
