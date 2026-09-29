package com.ventas.key.hexagonal.preferenciafiltro.infraestructura.dto;

import java.util.Map;

/** Solo los filtros de la pantalla (R4): ni el texto buscado ni la página. */
public record GuardarFiltrosRequest(Map<String, Object> filtros) {
}
