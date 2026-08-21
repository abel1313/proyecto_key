package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "red_social_negocio")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RedSocialNegocio extends BaseId {

    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    @Column(name = "url", length = 500, nullable = false)
    private String url;

    @Column(name = "activo", nullable = false)
    private boolean activo;
}
