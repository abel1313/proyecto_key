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
    @JoinColumn(name = "variante_id", nullable = false)
    private Variantes variante;

    @Column(nullable = false)
    private Integer cantidad;
    @Column(name = "precio_unitario", nullable = false)
    private Double precioUnitario;
    @Column(name = "sub_total", nullable = false)
    private Double subTotal;

    @ManyToOne
    @JoinColumn(name = "promocion_id")
    private Promocion promocion;


    /**
     * NO se guarda en esta tabla. Solo viaja del cobro al guardado: si la linea se cobro con precio de grupo, aqui va el precio que tenia
     * antes, y despues de guardar el pedido se registra en {@code detalle_pedido_precio_grupo} para poder regresarla si el pedido se edita.
     */
    @Transient
    private Double precioAntesDeGrupo;

    /** De dónde vino el descuento de {@link #precioAntesDeGrupo}: grupo de precio por cantidad o campaña. {@code null} = grupo. */
    @Transient
    private com.ventas.key.hexagonal.preciocantidad.dominio.modelo.OrigenDePrecio origenDelDescuento;
}
