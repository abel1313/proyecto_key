package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "detalle_venta_variantes")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DetalleVentaVariante extends BaseId {

    @ManyToOne
    @JoinColumn(name = "venta_id", nullable = false)
    @JsonIgnore
    private Venta venta;

    @ManyToOne
    @JoinColumn(name = "variante_id", nullable = false)
    private Variantes variante;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false)
    private Double precioUnitario;

    @Column(name = "sub_total", nullable = false)
    private Double subTotal;

    @Column(name = "precio_costo", nullable = false)
    private Double precioCosto;

    @Column(nullable = false)
    private Double ganancia;

    @Column(name = "fecha_venta", nullable = false)
    private LocalDate fechaVenta;

    /**
     * A qué tipo de precio se vendió la línea: VENTA, REBAJA, CAMPANA, GRUPO, PROMOCION u OTRO (migration_tipo_precio_venta.sql).
     * Null en las ventas anteriores a esa migración: el reporte las estima comparando el precio cobrado con los precios del producto.
     */
    @Column(name = "tipo_precio", length = 12)
    private String tipoPrecio;

    @ManyToOne
    @JoinColumn(name = "promocion_id")
    private Promocion promocion;
}