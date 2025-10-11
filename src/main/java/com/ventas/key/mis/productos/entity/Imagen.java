package com.ventas.key.mis.productos.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "imagen")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Imagen  extends BaseId{
    @Column(name = "bae_64")

    private String base64;

    private String extension;
    @Column(name = "nombre_imagen")
    private String nombreI1magen;

    @OneToMany(mappedBy = "imagen", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductoImagen> productosRelacionados;
}
