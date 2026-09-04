package com.ventas.key.mis.productos.models.pedidos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

// Respuesta de GET /v1/entregas-zona/{lugarEntregaId}/pendientes -- lunes/viernes son la semana
// de pedido que se está mostrando (siempre la actual), diaSugerido viene de
// LugarEntrega.diaEntregaSemanal ya resuelto a una fecha concreta (o null si esa zona no tiene
// día configurado todavía).
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class EntregaZonaSemanaResponse {
    private LocalDate lunes;
    private LocalDate viernes;
    private LocalDate fechaSugerida;
    private List<EntregaZonaPendienteDto> pedidos;
}
