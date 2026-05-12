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
}