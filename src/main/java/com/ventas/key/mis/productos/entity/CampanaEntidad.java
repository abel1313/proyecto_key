package com.ventas.key.mis.productos.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tabla {@code campana} (migration_campana.sql). Las reglas viven en {@code hexagonal/campana/dominio}; esto es solo el registro.
 * Vive aquí porque el escaneo de JPA solo cubre este paquete.
 */
@Entity
@Table(name = "campana")
@Getter
@Setter
@NoArgsConstructor
public class CampanaEntidad extends BaseId {

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false)
    private boolean activa = false;

    @Column(name = "meta_monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal metaMonto = BigDecimal.ZERO;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "campana", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CampanaProductoEntidad> productos = new ArrayList<>();
}
