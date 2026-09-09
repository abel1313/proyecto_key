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
    private LocalDate fechaPedido;
}
