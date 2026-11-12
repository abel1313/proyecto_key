package com.ventas.key.mis.productos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Tabla {@code compra_lote_producto}: un producto que llegó en el viaje (piezas, costo y flete por pieza). */
@Entity
@Table(name = "compra_lote_producto")
@Getter
@Setter
@NoArgsConstructor
public class CompraLoteProductoEntidad extends BaseId {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id", nullable = false)
    private CompraLoteEntidad lote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "piezas_compradas", nullable = false)
    private int piezasCompradas;

    @Column(name = "costo_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoUnitario = BigDecimal.ZERO;

    @Column(name = "flete_por_pieza", nullable = false, precision = 10, scale = 2)
    private BigDecimal fletePorPieza = BigDecimal.ZERO;
}
