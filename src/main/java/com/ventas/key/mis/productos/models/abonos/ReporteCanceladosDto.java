package com.ventas.key.mis.productos.models.abonos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
public class ReporteCanceladosDto {
    private Integer pedidoId;
    private String tipoPedido;
    private String cliente;
    private String telefono;
    private Double totalPedido;
    private Double totalPagado;
    // APARTADO: saldo a favor del cliente; FIADO: deuda incobrable
    private Double saldoAFavor;
    private Double deudaPendiente;
    private String motivo;
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate fechaPedido;
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate fechaCancelacion;
    private boolean puedeTransferir; // true si es APARTADO y totalPagado > 0
    private List<AbonoResponse> abonos;
}
