package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "detalle_pedidos")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DetallePedido extends  BaseId{

    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false)
    @JsonBackReference
    private Pedido pedido;

    @ManyToOne
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne
    @JoinColumn(name = "variante_id")
    private Variantes variante;

    private Integer cantidad;
    @Column(name = "precio_unitario", nullable = false)
    private Double precioUnitario;
    @Column(name = "sub_total", nullable = false)
    private Double subTotal;

}
