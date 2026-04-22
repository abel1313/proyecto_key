package com.ventas.key.mis.productos.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tipo_pago")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class TipoPago extends BaseId {

    @Column(name = "forma_pago", nullable = false)
    private String formaPago;
}