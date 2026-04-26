package com.ventas.key.mis.productos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "codigo_barras")
    private String codigoBarras;

}