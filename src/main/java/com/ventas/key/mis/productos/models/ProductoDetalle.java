package com.ventas.key.mis.productos.models;

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
    private int actualizarStock;
    private int eliminarStock;

    private Integer palabraClaveId;
    /** Al ACTUALIZAR un producto: true = quitarle la categoría. Sin esto, un palabraClaveId nulo significa «no tocar» (otras pantallas no mandan el campo). */
    private Boolean quitarPalabraClave;
    private CodigoBarraDetalle codigoBarras;
    private List<ImagenDTO> listImagenes;
    private Long imagenPrincipalId;
}
