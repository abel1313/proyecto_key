package com.ventas.key.mis.productos.models.abonos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EstadoCuentaDto {
    private Integer pedidoId;
    private String tipoPedido;
    private String estadoPedido;
    private String cliente;
    private String telefono;
    private Double totalPedido;
    private Double totalPagado;
    private Double saldo;
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate fechaPedido;
    private List<AbonoResponse> abonos;
}
