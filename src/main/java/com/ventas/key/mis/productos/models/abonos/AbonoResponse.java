package com.ventas.key.mis.productos.models.abonos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AbonoResponse {
    private Integer id;
    private Double monto;
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate fechaPago;
    private String metodoPago;
    private String nota;
    private Double montoDado;
    private Double cambio;
    // Solo presentes al registrar un abono (POST); null en GET de historial
    private String estadoPedido;
    private Double saldoRestante;
    // Resultado de notificaciones (null si no se solicitaron)
    private Boolean correoEnviado;
    private Boolean whatsappEnviado;
    private List<String> erroresEnvio;
}
