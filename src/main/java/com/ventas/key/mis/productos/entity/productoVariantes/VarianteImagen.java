package com.ventas.key.mis.productos.entity.productoVariantes;

import com.ventas.key.mis.productos.entity.BaseId;
import com.ventas.key.mis.productos.entity.Imagen;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "variante_imagen")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class VarianteImagen extends BaseId {

    @ManyToOne
    @JoinColumn(name = "variante_id")
    private Variantes variante;

    @ManyToOne
    @JoinColumn(name = "imagen_id")
    private Imagen imagen;
}