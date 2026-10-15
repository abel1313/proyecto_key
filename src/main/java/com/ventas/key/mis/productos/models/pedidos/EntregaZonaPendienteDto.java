package com.ventas.key.mis.productos.models.pedidos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class EntregaZonaPendienteDto {
    private Integer pedidoId;
    private String nombreCliente;
    private String correo;
    private Double total;
    /** Cuándo se hizo el pedido (el rango de la pantalla filtra por esta). */
    private LocalDate fechaPedido;
    /** Día y hora de entrega ya programados en esta pantalla; null si todavía no (2026-10-13). */
    private LocalDate fechaEntrega;
    private String horaEntrega;
}
