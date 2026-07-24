package com.ventas.key.mis.productos.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lugares_entrega")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class LugarEntrega extends BaseId {

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;
}
