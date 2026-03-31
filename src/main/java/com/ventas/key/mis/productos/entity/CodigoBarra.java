package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "codigo_barras")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CodigoBarra extends BaseId {

    @OneToOne(mappedBy = "codigoBarras")
    @JsonBackReference
    private Producto producto;


@Column( name = "codigo_barras")
private String codigoBarras;


}
