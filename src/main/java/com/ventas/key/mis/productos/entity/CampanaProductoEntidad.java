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

/** Tabla {@code campana_producto}: un juguete de la campaña y su precio de campaña. Un producto no se repite dentro de la misma campaña. */
@Entity
@Table(name = "campana_producto")
@Getter
@Setter
@NoArgsConstructor
public class CampanaProductoEntidad extends BaseId {

    @ManyToOne(optional = false)
    @JoinColumn(name = "campana_id", nullable = false)
    private CampanaEntidad campana;

    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "precio_campana", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioCampana;
}
