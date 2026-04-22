package com.ventas.key.mis.productos.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "detalle_pagos")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class DetallePago {

    @EmbeddedId
    private DetallePagoId id;

    @ManyToOne
    @MapsId("tipoPagoId")
    @JoinColumn(name = "tipo_pago_id")
    private TipoPago tipoPago;

    @ManyToOne
    @MapsId("tarifaTerminalId")
    @JoinColumn(name = "tarifa_terminal_id")
    private TarifaTerminal tarifaTerminal;

    @ManyToOne
    @MapsId("ivaId")
    @JoinColumn(name = "iva_id")
    private IvaTerminal ivaTerminal;
}