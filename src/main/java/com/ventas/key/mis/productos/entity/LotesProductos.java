package com.ventas.key.mis.productos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lotes_productos")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LotesProductos extends BaseId{

    @Column(name = "precio_unitario")
    private Double precioUnitario;
    @Column(name = "precio_rebaja")
    private Integer stock;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;


}
