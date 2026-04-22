package com.ventas.key.mis.productos.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "iva_terminal")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class IvaTerminal extends BaseId {

    @Column(nullable = false)
    private Double iva;

    @Column(nullable = false)
    private String descripcion;
}