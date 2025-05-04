package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "producto")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Producto  extends BaseId{

    private String nombre;
    @Column(name = "precio_costo")
    private Double precioCosto;
    private Double piezas;
    private String color;
    @Column(name = "precio_venta")
    private Double precioVenta;
    @Column(name = "precio_rebaja")
    private Double precioRebaja;
    private String descripcion;
    private Integer stock;
    private String marca;
    @Column(name = "contenido_neto")
    private String contenido;
    
    @ManyToOne
    @JoinColumn(name = "codigo_barras_id") //
     @JsonManagedReference
    private CodigoBarra codigoBarras;


    
}
