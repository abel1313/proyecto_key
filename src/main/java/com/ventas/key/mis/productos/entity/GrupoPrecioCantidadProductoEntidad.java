package com.ventas.key.mis.productos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Tabla {@code grupo_precio_cantidad_producto}: un producto del grupo y su precio de promoción. {@code producto_id} es ÚNICO: un
 * producto solo puede estar en un grupo (regla G2).
 */
@Entity
@Table(name = "grupo_precio_cantidad_producto")
@Getter
@Setter
@NoArgsConstructor
public class GrupoPrecioCantidadProductoEntidad extends BaseId {

    @ManyToOne(optional = false)
    @JoinColumn(name = "grupo_id", nullable = false)
    private GrupoPrecioCantidadEntidad grupo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id", nullable = false, unique = true)
    private Producto producto;

    @Column(name = "precio_promocion", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioPromocion;
}
