package com.ventas.key.hexagonal.reportecategoria.infraestructura.salida.archivo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lo que va en la cabecera y el pie del Excel y del PDF: el nombre del negocio, sus datos de contacto (los que estén capturados en «Datos legales»;
 * nunca se inventan) y quién y cuándo generó el reporte.
 */
public record EncabezadoReporte(String negocio, List<String> contacto, String generadoPor, LocalDateTime generado) {

    public static final String NOMBRE_NEGOCIO = "Novedades Jade";
    public static final String TITULO = "Reporte por categoría";

    public EncabezadoReporte {
        negocio = negocio == null || negocio.isBlank() ? NOMBRE_NEGOCIO : negocio;
        contacto = contacto == null ? List.of() : List.copyOf(contacto);
        generado = generado == null ? LocalDateTime.now() : generado;
    }

    /** Los datos de contacto en una sola línea («Responsable · domicilio · tel. … · correo»); vacío si no hay ninguno. */
    public String lineaContacto() {
        return String.join("  ·  ", contacto);
    }
}
