package com.ventas.key.hexagonal.preferenciafiltro.infraestructura.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record FiltrosGuardadosResponse(String pantalla, Map<String, Object> filtros, LocalDateTime actualizado) {
}
