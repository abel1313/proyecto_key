package com.ventas.key.mis.productos.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class ProductoResumen {
    private int idProducto;
    private String nombre;
    private String descripcion;
    private int stock;
    private Double getPrecioVenta;
    private String codigoBarras;
    private PalabraClaveResumenDto palabraClave;

    public ProductoResumen(int idProducto, String nombre, String descripcion, int stock,
                           Double getPrecioVenta, String codigoBarras,
                           Integer pkId, String pkNombre) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.stock = stock;
        this.getPrecioVenta = getPrecioVenta;
        this.codigoBarras = codigoBarras;
        this.palabraClave = pkId != null ? new PalabraClaveResumenDto(pkId, pkNombre) : null;
    }
}
