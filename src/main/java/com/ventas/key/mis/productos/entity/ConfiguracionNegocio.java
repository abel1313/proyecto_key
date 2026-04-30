package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_negocio")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ConfiguracionNegocio extends BaseId {

    private boolean abierto;

    @Column(name = "abierto_desde")
    private LocalDateTime abiertoDesde;

    @Column(name = "cerrado_desde")
    private LocalDateTime cerradoDesde;

    @Column(name = "hora_apertura")
    private LocalDateTime horaApertura;

    @Column(name = "hora_cierre")
    private LocalDateTime horaCierre;

    @Column(name = "whatsapp_url", length = 500)
    private String whatsappUrl;

    @Column(name = "facebook_url", length = 500)
    private String facebookUrl;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;
}