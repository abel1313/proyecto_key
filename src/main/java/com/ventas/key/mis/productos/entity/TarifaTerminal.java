package com.ventas.key.mis.productos.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tarifa_terminal")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class TarifaTerminal extends BaseId {

    @Column(nullable = false)
    private Double tarifa;

    @Column(nullable = false)
    private String descripcion;
}