package com.ventas.key.hexagonal.datoslegales.infraestructura.dto;

import com.ventas.key.hexagonal.datoslegales.dominio.modelo.DatosLegales;

import java.util.List;

/** Datos del negocio + qué falta para cumplir la LFPC art. 76 bis III. */
public record DatosLegalesResponse(
        String nombreResponsable,
        String rfc,
        String domicilio,
        String telefono,
        String correo,
        String horarioAtencion,
        List<String> faltan,
        boolean completos) {

    public static DatosLegalesResponse de(DatosLegales d) {
        return new DatosLegalesResponse(d.nombreResponsable(), d.rfc(), d.domicilio(), d.telefono(),
                d.correo(), d.horarioAtencion(), d.faltantes(), d.completos());
    }
}
