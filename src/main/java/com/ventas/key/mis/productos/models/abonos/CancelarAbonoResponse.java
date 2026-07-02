package com.ventas.key.mis.productos.models.abonos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CancelarAbonoResponse {
    private Integer pedidoId;
    private String tipoPedido;
    private String estadoPedido;
    private Double totalPagado;
    private Double totalPendiente;
    private boolean stockDevuelto;
    private String mensaje;
    private Boolean correoEnviado;
    private Boolean whatsappEnviado;
    private List<String> erroresEnvio;
}
