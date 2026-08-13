package com.ventas.key.mis.productos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Representa la "especie" de flor (ej. "Rosa eterna"), no un color especifico -- los colores
// vendibles de esta especie viven en ColorFlor. El precio por flor y las cantidades validas
// (CantidadFlorValida) son por especie: todos los colores de una misma especie cuestan lo mismo
// y comparten la misma tabla de "que cantidades forman bien el circulo".
@Entity
@Table(name = "tipo_flor")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TipoFlor extends BaseId {

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "precio_por_flor", nullable = false)
    private Double precioPorFlor;

    @Column(name = "precio_costo")
    private Double precioCosto;

    @Column(nullable = false)
    private Boolean activo = true;
}
