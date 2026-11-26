package com.ventas.key.hexagonal.whatsapp.infraestructura.entrada.rest.dto;

import java.util.List;

/** Plantilla aprobada por Meta a enviar: su nombre exacto, el idioma (por defecto es_MX) y los valores de {{1}}, {{2}}… en orden. */
public record PlantillaRequest(String nombre, String idioma, List<String> parametros) {
}
