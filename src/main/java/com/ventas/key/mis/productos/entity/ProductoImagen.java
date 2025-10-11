package com.ventas.key.mis.productos.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "producto_imagenes")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ProductoImagen extends BaseId{

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne
    @JoinColumn(name = "imagen_id")
    private Imagen imagen;
}
