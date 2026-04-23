package com.ventas.key.mis.productos.entity.productoVariantes;

import com.ventas.key.mis.productos.entity.BaseId;
import com.ventas.key.mis.productos.entity.Producto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "variantes")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Variantes  extends BaseId {

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    private String talla;

    private String descripcion;

    private String color;

    private String presentacion;

    private int stock;

    private String marca;

    @Column(name = "contenido_neto")
    private String contenidoNeto;

}
