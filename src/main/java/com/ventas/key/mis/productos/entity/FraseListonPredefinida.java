package com.ventas.key.mis.productos.entity;

import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
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
@Table(name = "frase_liston_predefinida")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FraseListonPredefinida extends BaseId {

    @Column(nullable = false, length = 200)
    private String texto;

    @Column(nullable = false)
    private Double precio;

    @Column(nullable = false)
    private Boolean activo = true;

    // Variante "sombra" -- ver comentario en TipoFlor.variante.
    @ManyToOne
    @JoinColumn(name = "variante_id")
    private Variantes variante;
}
