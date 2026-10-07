package com.ventas.key.hexagonal.datoslegales.infraestructura.dto;

/** Lo que manda Configuración del negocio → Datos legales. Todo opcional. */
public record DatosLegalesRequest(
        String nombreResponsable,
        String rfc,
        String domicilio,
        String telefono,
        String correo,
        String horarioAtencion) {
}
