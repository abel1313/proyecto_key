package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ventas.key.mis.productos.Utils.TextoNormalizado;
import jakarta.persistence.*;
import lombok.*;

/**
 * PalabraClave = lo que el usuario final ve como "CATEGORIA" en la app.
 *
 * Los nombres de la API no cambian por esto. Ver TAXONOMIA_NOMBRES_BACK.md.
 */
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

    @PrePersist
    @PreUpdate
    private void normalizarNombre() {
        this.nombre = TextoNormalizado.mayusculas(this.nombre);
    }
}
