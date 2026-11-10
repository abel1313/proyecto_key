package com.ventas.key.hexagonal.campana.dominio.puerto.entrada;

import com.ventas.key.hexagonal.campana.dominio.modelo.ReporteDeCampana;

/** [Hexagonal: Driving Port] El reporte de una campaña: quién lleva cuánto y quién ya llegó a la meta. Solo lee. */
public interface ConsultarReporteDeCampanaCasoUso {

    ReporteDeCampana reporte(int campanaId);
}
