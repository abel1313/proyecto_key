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

    @Column(name = "instagram_url", length = 500)
    private String instagramUrl;

    @Column(name = "tiktok_url", length = 500)
    private String tiktokUrl;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    /** Umbral de "stock bajo" para el digest diario (StockBajoScheduler). Null = usa UMBRAL_DEFAULT. */
    @Column(name = "umbral_stock_bajo")
    private Integer umbralStockBajo;

    /** Direccion del local tal como se le muestra al cliente en login y registro. */
    @Column(name = "direccion", length = 255)
    private String direccion;

    // Punto exacto del local. Se captura desde Configuracion del negocio con el mismo selector de
    // mapa del punto de encuentro de las entregas. Con estos dos el front arma el link de "Como
    // llegar" a Google Maps; mientras alguno sea null, login y registro no muestran nada.
    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

    public static final int UMBRAL_DEFAULT_STOCK_BAJO = 5;
}