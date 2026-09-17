package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "palabra_clave")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PalabraClave extends BaseId {

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;
}
