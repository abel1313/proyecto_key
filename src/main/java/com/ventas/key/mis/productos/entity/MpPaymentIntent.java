package com.ventas.key.mis.productos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mp_payment_intent")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MpPaymentIntent extends BaseId {

    @Column(name = "intent_id", nullable = false, unique = true)
    private String intentId;

    @Column(name = "pedido_id", nullable = false)
    private Integer pedidoId;

    @Column(name = "cliente_id", nullable = false)
    private Integer clienteId;

    @Column(nullable = false)
    private Double monto;

    @Column(nullable = false)
    private Integer cuotas;

    @Column(nullable = false)
    private String estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_update")
    private LocalDateTime fechaUpdate;
}