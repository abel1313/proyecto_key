package com.ventas.key.mis.productos.models.abonos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AbonoResponse {
    private Integer id;
    private Double monto;
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate fechaPago;
    private String metodoPago;
    private String nota;
    // Solo presentes al registrar un abono (POST); null en GET de historial
    private String estadoPedido;
    private Double saldoRestante;
}
