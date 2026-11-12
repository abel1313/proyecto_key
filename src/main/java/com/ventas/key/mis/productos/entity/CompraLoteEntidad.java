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
 * Tabla {@code compra_lote} (migration_flete_lote.sql): un viaje de compra. Las reglas viven en {@code hexagonal/flete/dominio}; esto es solo el registro.
 * Vive aquí porque el escaneo de JPA solo cubre este paquete.
 */
@Entity
@Table(name = "compra_lote")
@Getter
@Setter
@NoArgsConstructor
public class CompraLoteEntidad extends BaseId {

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "fecha_compra", nullable = false)
    private LocalDate fechaCompra;

    @Column(name = "flete_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal fleteTotal = BigDecimal.ZERO;

    @Column(length = 255)
    private String nota;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "lote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompraLoteGastoEntidad> gastos = new ArrayList<>();

    @OneToMany(mappedBy = "lote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompraLoteProductoEntidad> productos = new ArrayList<>();
}
