package com.ventas.key.mis.productos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tabla {@code producto_calculo_precio} (migration_calculo_precio.sql): historial de los cálculos de precio de cada producto.
 * Las reglas viven en {@code hexagonal/calculoprecio/dominio}; esto es solo el registro (el escaneo de JPA solo cubre este paquete).
 */
@Entity
@Table(name = "producto_calculo_precio")
@Getter
@Setter
@NoArgsConstructor
public class ProductoCalculoPrecioEntidad extends BaseId {

    @Column(name = "producto_id", nullable = false)
    private Integer productoId;

    @Column(name = "costo_compra", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoCompra;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal transporte = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal otros = BigDecimal.ZERO;

    @Column(name = "comision_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal comisionPct = BigDecimal.ZERO;

    @Column(name = "comision_descripcion", length = 100)
    private String comisionDescripcion;

    @Column(name = "margen_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal margenPct = BigDecimal.ZERO;

    @Column(name = "precio_calculado", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioCalculado;

    @Column(name = "precio_cobrado", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioCobrado;

    @Column(name = "actualizo_precio_compra", nullable = false)
    private boolean actualizoPrecioCompra = false;

    @Column(length = 300)
    private String nota;

    @Column(length = 100)
    private String usuario;

    @Column(nullable = false)
    private LocalDateTime fecha;
}
