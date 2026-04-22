package com.ventas.key.mis.productos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter @Setter @AllArgsConstructor @NoArgsConstructor @EqualsAndHashCode
public class DetallePagoId implements Serializable {

    @Column(name = "tipo_pago_id")
    private Integer tipoPagoId;

    @Column(name = "tarifa_terminal_id")
    private Integer tarifaTerminalId;

    @Column(name = "iva_id")
    private Integer ivaId;
}