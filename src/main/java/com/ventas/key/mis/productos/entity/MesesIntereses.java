package com.ventas.key.mis.productos.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meses_intereses")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class MesesIntereses extends BaseId {

    @Column(nullable = false)
    private String meses;

    @Column(nullable = false)
    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "iva_id")
    private IvaTerminal ivaTerminal;

    @ManyToOne
    @JoinColumn(name = "tarifa_id")
    private TarifaTerminal tarifaTerminal;
}