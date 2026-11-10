package com.ventas.key.mis.productos.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tabla {@code grupo_precio_cantidad} (migration_grupo_precio_cantidad.sql). Las reglas viven en
 * {@code hexagonal/preciocantidad/dominio}; esto es solo el registro. Vive aquí porque el escaneo de JPA solo cubre este paquete.
 */
@Entity
@Table(name = "grupo_precio_cantidad")
@Getter
@Setter
@NoArgsConstructor
public class GrupoPrecioCantidadEntidad extends BaseId {

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "minimo_piezas", nullable = false)
    private int minimoPiezas;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GrupoPrecioCantidadProductoEntidad> productos = new ArrayList<>();
}
