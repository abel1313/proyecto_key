package com.ventas.key.mis.productos.entity.productoVariantes;

import com.ventas.key.mis.productos.entity.BaseId;
import com.ventas.key.mis.productos.entity.Imagen;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "variante_imagen")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class VarianteImagen extends BaseId {

    @ManyToOne
    @JoinColumn(name = "variante_id")
    private Variantes variante;

    @ManyToOne
    @JoinColumn(name = "imagen_id")
    private Imagen imagen;
}