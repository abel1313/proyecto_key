package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
@Table(name = "promocion_detalle")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PromocionDetalle extends BaseId {

    @ManyToOne
    @JoinColumn(name = "promocion_id", nullable = false)
    @JsonBackReference
    private Promocion promocion;

    @ManyToOne
    @JoinColumn(name = "variante_id", nullable = false)
    private Variantes variante;

    @Column(nullable = false)
    private Integer cantidad = 1;

    @Column(name = "precio_en_promocion", nullable = false)
    private Double precioEnPromocion;
}
