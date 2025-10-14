package com.ventas.key.mis.productos.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.ventas.key.mis.productos.entity.CodigoBarra;

import com.ventas.key.mis.productos.entity.Imagen;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ProductoDetalle {

    private Integer id;
    private String nombre;
    private Double precioCosto;
    private Double piezas;
    private String color;
    private Double precioVenta;
    private Double precioRebaja;
    private String descripcion;
    private Integer stock;
    private String marca;
    private String contenido;

    private CodigoBarraDetalle codigoBarras;
    private List<ImagenDTO> listImagenes;
}
