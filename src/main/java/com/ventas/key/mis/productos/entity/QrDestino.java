package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "qr_destino")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class QrDestino extends BaseId {

    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    @Column(name = "url", length = 1000, nullable = false)
    private String url;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    @Column(name = "icono", length = 10)
    private String icono;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "orden", nullable = false)
    private Integer orden;
}
