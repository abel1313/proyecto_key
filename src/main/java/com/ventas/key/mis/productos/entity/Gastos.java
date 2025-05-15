package com.ventas.key.mis.productos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "gastos_surtir")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Gastos extends BaseId{

    @Column(name = "descripcion_gasto")
    private String descripcionGasto;
    @Column(name = "precio_gasto")
    private Double precioGasto;

}
