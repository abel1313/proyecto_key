package com.ventas.key.mis.productos.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
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

@OneToMany(mappedBy = "codigoBarras")
@JsonBackReference
private List<Producto> productos;


@Column( name = "codigo_barras")
private String codigoBarras;


}
