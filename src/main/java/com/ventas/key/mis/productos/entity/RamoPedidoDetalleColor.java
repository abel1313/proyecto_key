package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ramo_pedido_detalle_color")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RamoPedidoDetalleColor extends BaseId {

    @ManyToOne
    @JoinColumn(name = "ramo_pedido_detalle_id", nullable = false)
    @JsonBackReference
    private RamoPedidoDetalle ramoPedidoDetalle;

    @ManyToOne
    @JoinColumn(name = "color_flor_id", nullable = false)
    private ColorFlor colorFlor;

    @Column(nullable = false)
    private Integer cantidad;
}
